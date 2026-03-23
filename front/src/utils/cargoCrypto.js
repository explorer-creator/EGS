/**
 * 与 D:\ft\rc4_explain.py 一致的 RC4（KSA + PRGA）
 * 仅用于演示/教学，生产环境请使用 AES-GCM 等现代算法。
 */
export function rc4Ksa(keyBytes) {
  const keyLen = keyBytes.length
  const S = Array.from({ length: 256 }, (_, i) => i)
  let j = 0
  for (let i = 0; i < 256; i++) {
    j = (j + S[i] + keyBytes[i % keyLen]) % 256
    const t = S[i]
    S[i] = S[j]
    S[j] = t
  }
  return S
}

export function rc4Prga(S, length) {
  const keystream = []
  let i = 0
  let j = 0
  for (let n = 0; n < length; n++) {
    i = (i + 1) % 256
    j = (j + S[i]) % 256
    const t = S[i]
    S[i] = S[j]
    S[j] = t
    keystream.push(S[(S[i] + S[j]) % 256])
  }
  return keystream
}

/** @param {Uint8Array|string} data @param {Uint8Array|string} key */
export function rc4Encrypt(data, key) {
  const enc = new TextEncoder()
  const dataBytes = typeof data === 'string' ? enc.encode(data) : data
  const keyBytes = typeof key === 'string' ? enc.encode(key) : key
  const S = rc4Ksa(keyBytes)
  const ks = rc4Prga(S, dataBytes.length)
  const out = new Uint8Array(dataBytes.length)
  for (let i = 0; i < dataBytes.length; i++) {
    out[i] = dataBytes[i] ^ ks[i]
  }
  return out
}

export function bytesToHex(buf) {
  return [...buf].map((b) => b.toString(16).padStart(2, '0')).join('')
}

export function hexToBytes(hex) {
  const s = hex.replace(/\s/g, '')
  if (s.length % 2) throw new Error('十六进制长度须为偶数')
  const out = new Uint8Array(s.length / 2)
  for (let i = 0; i < out.length; i++) {
    out[i] = parseInt(s.slice(i * 2, i * 2 + 2), 16)
  }
  return out
}

/** AES-128 密钥：16 字节（与 ft/aes128_example.py 一致用字符串截断/填充） */
export function aesKeyFromString(str) {
  const enc = new TextEncoder()
  const raw = enc.encode(str)
  const key = new Uint8Array(16)
  key.set(raw.slice(0, 16))
  return key
}

/**
 * AES-128-CBC + PKCS#7，输出：IV(16) || ciphertext（与常见 Python 行为一致）
 */
export async function aes128CbcEncrypt(plaintextUtf8, keyString) {
  const keyBytes = aesKeyFromString(keyString)
  const key = await crypto.subtle.importKey('raw', keyBytes, 'AES-CBC', false, ['encrypt'])
  const iv = crypto.getRandomValues(new Uint8Array(16))
  const enc = new TextEncoder()
  const plain = enc.encode(plaintextUtf8)
  const cipherBuf = await crypto.subtle.encrypt({ name: 'AES-CBC', iv }, key, plain)
  const cipher = new Uint8Array(cipherBuf)
  const out = new Uint8Array(iv.length + cipher.length)
  out.set(iv, 0)
  out.set(cipher, iv.length)
  return out
}

export async function aes128CbcDecrypt(ivAndCipher, keyString) {
  const keyBytes = aesKeyFromString(keyString)
  const key = await crypto.subtle.importKey('raw', keyBytes, 'AES-CBC', false, ['decrypt'])
  const iv = ivAndCipher.slice(0, 16)
  const cipher = ivAndCipher.slice(16)
  const plainBuf = await crypto.subtle.decrypt({ name: 'AES-CBC', iv }, key, cipher)
  return new TextDecoder().decode(plainBuf)
}

/** SHA-256 hex，用于区块链展示 */
export async function sha256Hex(text) {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
  return bytesToHex(new Uint8Array(buf))
}
