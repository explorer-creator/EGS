<template>
  <div class="ccp">
    <div class="ccp__head">
      <span class="ccp__badge">链上隐私</span>
      <strong>货物信息加密</strong>
      <span class="ccp__hint">算法源自本地 D:\ft（AES-128 / RC4 演示）</span>
    </div>

    <el-tabs v-model="mode" type="card" class="ccp__tabs">
      <el-tab-pane label="AES-128-CBC" name="aes">
        <p class="ccp__tip">浏览器 Web Crypto 实现；密钥取 UTF-8 前 16 字节；密文格式为 IV‖Ciphertext（PKCS#7）。</p>
        <el-form label-position="top" size="mini">
          <el-form-item label="密钥（16 字符，不足补空格）">
            <el-input v-model="aesKey" maxlength="32" show-word-limit placeholder="如 1234567890123456" />
          </el-form-item>
          <el-form-item label="货物明文（JSON / 文本）">
            <el-input v-model="plain" type="textarea" :rows="4" placeholder='{"运单":"SF123","品名":"电子元件","件数":42}' />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" :loading="busy" @click="doAesEnc">加密</el-button>
            <el-button size="small" :loading="busy" @click="doAesDec">解密下方密文</el-button>
          </el-form-item>
          <el-form-item label="密文（hex，IV+密文）">
            <el-input v-model="aesCipherHex" type="textarea" :rows="3" placeholder="加密后或粘贴 hex 解密" />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="RC4 流密码" name="rc4">
        <p class="ccp__tip">与 rc4_explain.py 相同 KSA/PRGA；密钥示例「32250837」。</p>
        <el-form label-position="top" size="mini">
          <el-form-item label="密钥（字符串）">
            <el-input v-model="rc4Key" placeholder="32250837" />
          </el-form-item>
          <el-form-item label="货物明文">
            <el-input v-model="plain" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="doRc4Enc">加密 → hex</el-button>
            <el-button size="small" @click="doRc4Dec">解密 hex</el-button>
          </el-form-item>
          <el-form-item label="RC4 密文（hex）">
            <el-input v-model="rc4Hex" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <div v-if="msg" class="ccp__msg" :class="{ 'ccp__msg--err': msgErr }">{{ msg }}</div>
  </div>
</template>

<script>
import {
  rc4Encrypt,
  bytesToHex,
  hexToBytes,
  aes128CbcEncrypt,
  aes128CbcDecrypt
} from '../utils/cargoCrypto.js'

export default {
  name: 'CargoCryptoPanel',
  data() {
    return {
      mode: 'aes',
      aesKey: '1234567890123456',
      rc4Key: '32250837',
      plain: '{\n  "运单": "EGS-2026-0316",\n  "品名": "冷链医药箱",\n  "件数": 12,\n  "目的地": "阿拉木图"\n}',
      aesCipherHex: '',
      rc4Hex: '',
      busy: false,
      msg: '',
      msgErr: false
    }
  },
  methods: {
    toast(text, err = false) {
      this.msg = text
      this.msgErr = err
      setTimeout(() => {
        this.msg = ''
      }, 4200)
    },
    async doAesEnc() {
      this.busy = true
      try {
        const buf = await aes128CbcEncrypt(this.plain, this.aesKey)
        this.aesCipherHex = bytesToHex(buf)
        this.toast('AES-128-CBC 加密成功（已含随机 IV）')
      } catch (e) {
        this.toast(String(e.message || e), true)
      } finally {
        this.busy = false
      }
    },
    async doAesDec() {
      this.busy = true
      try {
        const raw = hexToBytes(this.aesCipherHex.trim())
        const text = await aes128CbcDecrypt(raw, this.aesKey)
        this.plain = text
        this.toast('解密成功')
      } catch (e) {
        this.toast('解密失败：' + (e.message || e), true)
      } finally {
        this.busy = false
      }
    },
    doRc4Enc() {
      try {
        const ct = rc4Encrypt(this.plain, this.rc4Key)
        this.rc4Hex = bytesToHex(ct)
        this.toast('RC4 加密完成')
      } catch (e) {
        this.toast(String(e.message || e), true)
      }
    },
    doRc4Dec() {
      try {
        const ct = hexToBytes(this.rc4Hex.trim())
        const pt = rc4Encrypt(ct, this.rc4Key)
        this.plain = new TextDecoder().decode(pt)
        this.toast('RC4 解密成功')
      } catch (e) {
        this.toast('解密失败：' + (e.message || e), true)
      }
    }
  }
}
</script>

<style scoped>
.ccp {
  pointer-events: auto;
  padding: 10px 12px 12px;
  border-radius: 12px;
  background: linear-gradient(165deg, rgba(15, 23, 42, 0.94) 0%, rgba(30, 27, 75, 0.9) 100%);
  border: 1px solid rgba(129, 140, 248, 0.35);
  box-shadow: 0 0 40px rgba(99, 102, 241, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  max-height: min(52vh, 420px);
  overflow: auto;
}
.ccp__head {
  margin-bottom: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #c7d2fe;
}
.ccp__head strong {
  font-size: 13px;
  color: #eef2ff;
  letter-spacing: 0.06em;
}
.ccp__badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #4f46e5, #7c3aed);
  color: #fff;
  font-weight: 700;
}
.ccp__hint {
  font-size: 10px;
  opacity: 0.75;
  color: #94a3b8;
}
.ccp__tip {
  margin: 0 0 8px;
  font-size: 11px;
  color: #a5b4fc;
  line-height: 1.45;
}
.ccp__tabs >>> .el-tabs__header {
  margin-bottom: 8px;
}
.ccp__tabs >>> .el-tabs__item {
  color: #94a3b8 !important;
  font-size: 12px;
}
.ccp__tabs >>> .el-tabs__item.is-active {
  color: #c7d2fe !important;
}
.ccp__msg {
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 11px;
  background: rgba(16, 185, 129, 0.15);
  color: #6ee7b7;
  border: 1px solid rgba(52, 211, 153, 0.35);
}
.ccp__msg--err {
  background: rgba(239, 68, 68, 0.12);
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.4);
}
</style>
