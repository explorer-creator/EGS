"""
server.py - FastAPI 后端服务
将 image_module_a.py 的核心功能封装为 RESTful API，同时挂载静态文件服务。

启动方式：
    uvicorn server:app --host 0.0.0.0 --port 8000 --reload

前端请求示例：
    POST http://localhost:8000/api/generate-image
    Body: { "keyword": "液压泵" }
"""

import os

# 可选：从 A 目录下 .env 加载 DASHSCOPE_API_KEY，便于通义万相兜底生图
try:
    from dotenv import load_dotenv

    load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))
except ImportError:
    pass
import time
import asyncio
from concurrent.futures import ThreadPoolExecutor
from typing import Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from PIL import Image

from image_module_a import get_material_image_path_a, _load_fallback_image

# ──────────────────────────────────────────────
# FastAPI 应用初始化
# ──────────────────────────────────────────────
app = FastAPI(
    title="工业图片物料生成 API",
    description="基于 image_module_a 的自动化图片获取与生成服务",
    version="1.0.0",
)

# ──────────────────────────────────────────────
# CORS 跨域配置（允许 Vue 开发服务器等前端访问）
# ──────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],          # 生产环境建议替换为你的前端域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ──────────────────────────────────────────────
# 挂载静态文件目录（前端可通过 URL 直接访问生成的图片）
# 例如：http://localhost:8000/output/液压泵_1710000000.jpg
# ──────────────────────────────────────────────
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "output")
os.makedirs(OUTPUT_DIR, exist_ok=True)
app.mount("/output", StaticFiles(directory=OUTPUT_DIR), name="output")

# ──────────────────────────────────────────────
# 请求体模型
# ──────────────────────────────────────────────
class GenerateRequest(BaseModel):
    keyword: str = "工业产品"


# ──────────────────────────────────────────────
# 线程池：让同步阻塞的 image_module_a 不会阻塞事件循环
# ──────────────────────────────────────────────
_executor = ThreadPoolExecutor(max_workers=4)


# ──────────────────────────────────────────────
# API 路由
# ──────────────────────────────────────────────

@app.get("/")
async def root():
    """健康检查"""
    return {"status": "ok", "message": "工业图片物料生成 API 运行中"}


@app.post("/api/generate-image")
async def generate_image(body: GenerateRequest):
    """
    接收关键词，调用 image_module_a 的完整流水线（爬取 → 校验 → AI生成兜底），
    返回可供前端直接 <img src="..."> 使用的图片 URL。

    请求体：
        { "keyword": "液压泵" }

    成功响应：
        {
            "status": "ok",
            "keyword": "液压泵",
            "image_url": "http://localhost:8000/output/液压泵_1710000000.jpg",
            "filename": "液压泵_1710000000.jpg"
        }
    """
    keyword = body.keyword.strip()
    if not keyword:
        raise HTTPException(status_code=400, detail="keyword 不能为空")

    try:
        # 在独立线程池中运行同步阻塞函数，避免卡住 asyncio 事件循环
        loop = asyncio.get_event_loop()
        img_path: Optional[str] = await loop.run_in_executor(
            _executor,
            get_material_image_path_a,
            keyword,
        )
    except Exception as exc:
        # 线程内已尽量兜底；仍失败时写占位图，避免前端总见 500
        img_path = None
        err_note = str(exc)
    else:
        err_note = ""

    if not img_path or not os.path.exists(img_path):
        _base = os.path.dirname(os.path.abspath(__file__))
        _out = os.path.join(_base, "output")
        os.makedirs(_out, exist_ok=True)
        emergency = os.path.join(_out, f"emergency_{int(time.time())}.jpg")
        try:
            _im = _load_fallback_image()
            _im.save(emergency, "JPEG", quality=90)
            img_path = emergency
        except Exception as ex2:
            # 最后一层：纯色图，避免前端总见 HTTP 500（依赖 Pillow 已安装）
            try:
                Image.new("RGB", (800, 800), (235, 236, 240)).save(emergency, "JPEG", quality=88)
                img_path = emergency
            except Exception as ex3:
                raise HTTPException(
                    status_code=500,
                    detail=f"图片生成失败（请检查 Pillow 与 output 目录权限）: {err_note or ex2} / {ex3}",
                ) from ex3

    filename = os.path.basename(img_path)
    # 构造前端可访问的图片 URL（通过上面挂载的静态文件服务）
    image_url = f"/output/{filename}"

    return JSONResponse(content={
        "status": "ok",
        "keyword": keyword,
        "image_url": image_url,   # 相对路径，前端拼接 baseURL 即可
        "filename": filename,
    })


# ──────────────────────────────────────────────
# 直接运行入口（也可用 uvicorn 启动）
# ──────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=True)
