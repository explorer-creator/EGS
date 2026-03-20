# -*- coding: utf-8 -*-
"""应用配置"""
import os


def get_llm_api_key() -> str:
    return os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY", "")


def get_llm_base_url() -> str:
    return os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1").rstrip("/")


def get_llm_model() -> str:
    return os.getenv("LLM_MODEL", "qwen-plus")


def get_cors_origins() -> list[str]:
    s = os.getenv("CORS_ORIGINS", "http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080")
    return [x.strip() for x in s.split(",") if x.strip()]
