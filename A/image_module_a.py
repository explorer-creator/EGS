import os
import random
import re
import time
import json
import requests
from bs4 import BeautifulSoup
from concurrent.futures import ThreadPoolExecutor, as_completed
from io import BytesIO
from PIL import Image, ImageChops, ImageStat, ImageFilter
import dashscope
from urllib.parse import quote

# 与 server.py 工作目录无关：一律相对本文件目录读写，避免 500（路径无效）
_MODULE_DIR = os.path.dirname(os.path.abspath(__file__))
_DEFAULT_DIR = os.path.join(_MODULE_DIR, "assets", "materials")
# 行星减速器示意图（用户提供的兜底配图，PNG）
PLANETARY_FALLBACK_PATH = os.path.join(_DEFAULT_DIR, "planetary_fallback.png")
DEFAULT_IMAGE_PATH = os.path.join(_DEFAULT_DIR, "default.jpg")
_OUTPUT_DIR = os.path.join(_MODULE_DIR, "output")


def _load_fallback_image():
    """优先使用仓库内行星减速器示意图，其次 default.jpg，最后生成灰底图。"""
    if os.path.isfile(PLANETARY_FALLBACK_PATH):
        try:
            im = Image.open(PLANETARY_FALLBACK_PATH)
            return im.convert("RGB")
        except Exception as e:
            print(f"[fallback] 无法打开行星示意图: {e}")
    if os.path.isfile(DEFAULT_IMAGE_PATH):
        try:
            im = Image.open(DEFAULT_IMAGE_PATH)
            return im.convert("RGB")
        except Exception as e:
            print(f"[fallback] 无法打开 default.jpg: {e}")
    os.makedirs(_DEFAULT_DIR, exist_ok=True)
    img = Image.new("RGB", (800, 800), color=(240, 240, 240))
    img.save(DEFAULT_IMAGE_PATH, "JPEG", quality=92)
    return img


def _write_fallback_jpeg(path: str) -> str:
    """将兜底图写入目标路径（JPEG）。"""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img = _load_fallback_image()
    img.save(path, "JPEG", quality=92)
    return os.path.abspath(path)


def _safe_filename_part(keyword: str) -> str:
    s = re.sub(r'[\\/:*?"<>|\s]+', "_", (keyword or "kw").strip())[:80]
    return s or "material"


class ImageProcessor:
    @staticmethod
    def crop_white_borders(image):
        """智能去白边 (Smart Crop) - 优化版，激进裁切以打破构图同质化"""
        if image.mode != "RGB":
            image = image.convert("RGB")
        bg = Image.new(image.mode, image.size, (255, 255, 255))
        diff = ImageChops.difference(image, bg)
        
        # 更加激进的阈值：颜色偏差在 40 以内的近似白边也抹为纯黑(背景)
        diff = diff.point(lambda p: 0 if p < 40 else p)
        bbox = diff.getbbox()
        
        if bbox:
            left, upper, right, lower = bbox
            w, h = image.size
            
            # Jitter 随机裁切干扰：向内额外收缩 0% ~ 4% 的像素，强制每张图边缘视觉不同
            left_shrink = int(w * random.uniform(0, 0.04))
            upper_shrink = int(h * random.uniform(0, 0.04))
            right_shrink = int(w * random.uniform(0, 0.04))
            lower_shrink = int(h * random.uniform(0, 0.04))
            
            left += left_shrink
            upper += upper_shrink
            right -= right_shrink
            lower -= lower_shrink
            
            if left < right and upper < lower:
                return image.crop((left, upper, right, lower))
            return image.crop(bbox)
            
        return image

    @staticmethod
    def calc_image_hash(image):
        """哈希校验: 生成 aHash (Average Hash) 简易指纹"""
        img = image.convert("L").resize((8, 8), Image.Resampling.LANCZOS)
        pixels = list(img.getdata())
        avg = sum(pixels) / len(pixels)
        bits = "".join(['1' if p > avg else '0' for p in pixels])
        return int(bits, 2)

    @staticmethod
    def is_similar(hash1, hash2, threshold=5):
        """汉明距离算相似度，如果不同位 <= threshold，则认定高度相似"""
        diff = bin(hash1 ^ hash2).count('1')
        return diff <= threshold

    @staticmethod
    def check_skin_tone(image):
        """肤色过滤: >12%视为有人"""
        ycbcr_img = image.convert('YCbCr')
        sm_img = ycbcr_img.copy()
        sm_img.thumbnail((150, 150))
        w_sm, h_sm = sm_img.size
        
        skin_pixels = 0
        total_pixels = w_sm * h_sm
        for y in range(h_sm):
            for x in range(w_sm):
                pixel = sm_img.getpixel((x, y))
                y_val, cb, cr = pixel[0], pixel[1], pixel[2]
                if 77 <= cb <= 127 and 133 <= cr <= 173:
                    skin_pixels += 1
        return (skin_pixels / total_pixels) <= 0.12

    @staticmethod
    def check_sharpness(image):
        """计算拉普拉斯方差代表清晰度，返回方差值"""
        grayscale = image.convert('L')
        sm_img = grayscale.copy()
        sm_img.thumbnail((300, 300)) 
        edges = sm_img.filter(ImageFilter.FIND_EDGES)
        stat = ImageStat.Stat(edges)
        return stat.var[0]

    @staticmethod
    def check_size(image):
        w, h = image.size
        return w >= 200 and h >= 200
        
    @classmethod
    def validate_and_process(cls, img_data):
        try:
            image = Image.open(BytesIO(img_data))
            image.verify()
            image = Image.open(BytesIO(img_data))
            
            if not cls.check_size(image):
                return None
                
            variance = cls.check_sharpness(image)
            if variance < 50:
                return None
                
            if not cls.check_skin_tone(image):
                return None
            
            processed_img = cls.crop_white_borders(image)
            
            # 评分随机化：打破“同分同质”僵局
            base_score = variance
            random_weight = random.uniform(0, 0.5) * 100 
            final_score = base_score + random_weight
            
            return processed_img, final_score
        except Exception:
            return None


class Scraper:
    def __init__(self, keyword):
        self.keyword = keyword
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        }

    def scrape_baidu(self):
        urls = []
        try:
            url = f"https://image.baidu.com/search/index?tn=baiduimage&word={quote(self.keyword)}"
            res = requests.get(url, headers=self.headers, timeout=3)
            urls = re.findall(r'"objURL":"(.*?)"', res.text)
            if not urls:
                urls = re.findall(r'"thumbURL":"(.*?)"', res.text)
        except:
            pass
        return urls

    def scrape_bing(self):
        urls = []
        try:
            url = f"https://cn.bing.com/images/search?q={quote(self.keyword)}"
            res = requests.get(url, headers=self.headers, timeout=3)
            soup = BeautifulSoup(res.text, 'html.parser')
            for a in soup.find_all('a', class_='iusc'):
                m = a.get('m')
                if m:
                    try:
                        urls.append(json.loads(m).get('murl'))
                    except:
                        pass
        except:
            pass
        return [u for u in urls if u]

    def download_image(self, url):
        # 如果当前 url 已经处理过，直接跳过 (Session 级去重)
        if url in Coordinator._seen_urls:
            return None
        try:
            res = requests.get(url, headers=self.headers, timeout=3)
            if res.status_code == 200:
                result = ImageProcessor.validate_and_process(res.content)
                if result:
                    img, score = result
                    return img, score, url
        except:
            pass
        return None

    def get_first_valid_image(self):
        start_time = time.time()
        
        with ThreadPoolExecutor(max_workers=2) as executor:
            f1 = executor.submit(self.scrape_baidu)
            f2 = executor.submit(self.scrape_bing)
            urls = [u for u in (f1.result() + f2.result()) if u]
            
        # 在这之前可能有很多无效或重复的内容
        urls = list(set(urls))
        urls = urls[:20]
        
        # 强制随机干扰：随机丢弃前 1~2 个元素
        drop_num = random.randint(1, 2)
        urls = urls[drop_num:]
        
        # 打乱随机性
        random.shuffle(urls)
        
        valid_results = []
        with ThreadPoolExecutor(max_workers=5) as executor:
            futures = {executor.submit(self.download_image, url): url for url in urls}
            for future in as_completed(futures):
                if time.time() - start_time > 10:
                    break
                res = future.result()
                if res:
                    img, score, valid_url = res
                    valid_results.append((score, img, valid_url))
                    # 收集几个候选结果从中挑分数最高(含随机权重)图
                    if len(valid_results) >= 3 or (time.time() - start_time > 6):
                        break

        if valid_results:
            valid_results.sort(key=lambda x: x[0], reverse=True)
            _, best_img, best_url = valid_results[0]
            return best_img, best_url
            
        return None


class Generator:
    """
    通义万相（DashScope ImageSynthesis）兜底生图。
    多模型顺序回退；参数按 SDK 能力逐级降级（反向提示 / seed）。
    """

    _WANX_MODELS = (
        "wanx2.1-t2i-turbo",
        "wanx2.1-t2i-plus",
        "wanx-v1",
    )

    def __init__(self, keyword):
        self.keyword = keyword
        self.api_key = (os.getenv("DASHSCOPE_API_KEY") or "").strip()
        if self.api_key:
            dashscope.api_key = self.api_key

    def _wanx_call(self, model: str, prompt: str, negative: str, seed: int):
        """按参数兼容性多次尝试单次 ImageSynthesis.call，返回 response 或抛错。"""
        base = {
            "api_key": self.api_key,
            "model": model,
            "prompt": prompt,
            "n": 1,
            "size": "1024*1024",
        }
        attempts = [
            {**base, "negative_prompt": negative, "seed": seed},
            {**base, "seed": seed},
            {**base, "negative_prompt": negative},
            dict(base),
        ]
        last_exc = None
        for kw in attempts:
            try:
                return dashscope.ImageSynthesis.call(**kw)
            except TypeError as e:
                last_exc = e
                continue
        raise last_exc if last_exc else RuntimeError("ImageSynthesis.call 参数不兼容")

    def generate_image(self):
        if not self.api_key:
            print("[Warning] 未设置 DASHSCOPE_API_KEY，跳过通义万相生图兜底。")
            return None

        seed = random.randint(1000, 999999)
        prompt = (
            f"{self.keyword}, 工业产品/设备物料示意, 工业写实风格, 3D 渲染或高清摄影感, "
            f"纯白或浅灰背景, 无人物, 无手部, 无文字水印, 专业布光, 8k"
        )
        negative = "低清晰度,模糊,畸形,多余手指,人脸,文字,水印,杂乱背景"

        last_err = ""
        for model in self._WANX_MODELS:
            try:
                rsp = self._wanx_call(model, prompt, negative, seed)
                code = getattr(rsp, "status_code", None)
                if code != 200:
                    last_err = getattr(rsp, "message", None) or str(rsp)
                    print(f"[通义生图] {model} 失败: {last_err}")
                    continue

                out = getattr(rsp, "output", None)
                results = getattr(out, "results", None) if out is not None else None
                if not results:
                    last_err = getattr(rsp, "message", None) or "无 results"
                    print(f"[通义生图] {model} 无图: {last_err}")
                    continue

                url = getattr(results[0], "url", None)
                if not url:
                    continue

                res = requests.get(url, timeout=45)
                if res.status_code == 200:
                    print(f"[通义生图] 成功 model={model}")
                    return Image.open(BytesIO(res.content))
                last_err = f"下载图失败 HTTP {res.status_code}"
            except Exception as e:
                last_err = str(e)
                print(f"[通义生图] {model} 异常: {e}")

        print(f"[通义生图] 全部模型失败，最后信息: {last_err or 'unknown'}")
        return None


class Coordinator:
    # Session 级历史去重
    _seen_urls = set()
    _seen_hashes = set()
    _consecutive_dup_count = 0  # 连续重复/相似抓取计算

    def __init__(self, keyword):
        self.keyword = keyword

    def run(self):
        print(f"[{self.keyword}] 开始进入自动化物料获取流水线...")
        
        # 强制切换逻辑：如果已经连续 2 张图相似度过高，不再尝试，强制 AI
        if Coordinator._consecutive_dup_count >= 2:
            print(f"[{self.keyword}] 发现历史抓取图相似严重滞塞(连续 >= 2次)，直接强制触发 AI 生图打破同质化！")
            img = self.trigger_ai()
            # 成功触发 AI 后重置计数
            Coordinator._consecutive_dup_count = 0
            return img

        # 第一优先级：抓取
        start_time = time.time()
        scraper = Scraper(self.keyword)
        result = scraper.get_first_valid_image()
        
        if result and time.time() - start_time <= 10:
            img, url = result
            img_hash = ImageProcessor.calc_image_hash(img)
            
            # 图像后处理检测增强: 如果新图片指纹与之前的某个高度相似(不同<=5)，认定重复
            is_dup = any(ImageProcessor.is_similar(img_hash, sh) for sh in Coordinator._seen_hashes)
            
            if is_dup:
                print(f"[{self.keyword}] 警告：新抓取的图画与历史产出高度相似(哈希雷同)，拒绝应用！")
                Coordinator._consecutive_dup_count += 1
                
                if Coordinator._consecutive_dup_count >= 2:
                    print(f"[{self.keyword}] 连续 2 次抓去发生重复，马上触发 AI 生图接管...")
                    img = self.trigger_ai()
                    Coordinator._consecutive_dup_count = 0
                    return img
                else:
                    print(f"[{self.keyword}] 单次相似，本次退化为 AI 生成补救。")
                    return self.trigger_ai()
            else:
                # 全新有效图
                print(f"[{self.keyword}] 抓取成功！全新且校验完备的图。耗时: {time.time() - start_time:.2f}s")
                Coordinator._seen_urls.add(url)
                Coordinator._seen_hashes.add(img_hash)
                Coordinator._consecutive_dup_count = 0
                return img
            
        # 抓取耗时超 10s 或未获取到合格图片
        print(f"[{self.keyword}] 抓取未获取有效信息(或全部过滤)，切换 AI 生图...")
        return self.trigger_ai()

    def trigger_ai(self):
        generator = Generator(self.keyword)
        img = generator.generate_image()
        if img:
            print(f"[{self.keyword}] AI 生成成功！")
            img = ImageProcessor.crop_white_borders(img)
            # 同样记录 AI 生图的 hash
            img_hash = ImageProcessor.calc_image_hash(img)
            Coordinator._seen_hashes.add(img_hash)
            return img
            
        print(f"[{self.keyword}] API 调用失败，执行最终兜底返回默认图片...")
        return self._get_default_image()

    def _get_default_image(self):
        return _load_fallback_image()


def get_material_image_path_a(keyword):
    """
    返回绝对路径 JPEG；任一步失败时写入占位图，避免 FastAPI 返回 500。
    """
    safe_kw = _safe_filename_part(keyword)
    try:
        coordinator = Coordinator(keyword)
        img = coordinator.run()
        if img:
            os.makedirs(_OUTPUT_DIR, exist_ok=True)
            path = os.path.join(_OUTPUT_DIR, f"{safe_kw}_{int(time.time())}.jpg")
            if img.mode in ("RGBA", "P"):
                img = img.convert("RGB")
            img.save(path, "JPEG", quality=95)
            return os.path.abspath(path)
    except Exception as ex:
        print(f"[get_material_image_path_a] 流水线异常，使用占位图: {ex}")
    # 兜底：行星减速器示意图或灰底占位（爬取/AI 均失败或返回空时）
    try:
        os.makedirs(_OUTPUT_DIR, exist_ok=True)
        fallback = os.path.join(_OUTPUT_DIR, f"{safe_kw}_placeholder_{int(time.time())}.jpg")
        return _write_fallback_jpeg(fallback)
    except Exception as ex2:
        print(f"[get_material_image_path_a] 兜底失败: {ex2}")
        return None

if __name__ == "__main__":
    result_path = get_material_image_path_a("液压泵")
    print(f"-> 最终输出产物路径: {result_path}")
