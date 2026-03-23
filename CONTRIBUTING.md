# 贡献指南

感谢关注本项目。提交代码前请先阅读：

1. **[docs/Git协作开发指南.md](docs/Git协作开发指南.md)** — 分支、提交信息、PR 流程  
2. **[docs/本地开发指南.md](docs/本地开发指南.md)** — Mock 模式、启动命令、常见问题  
3. 勿提交 **密钥、本地配置**（如 `application-local.yml`、含真实 Key 的 `.env`）

## Pull Request

- 尽量关联 Issue 或简要说明改动动机  
- 前端改动请在 `front/` 下通过 `npm run build` 验证无报错  
- Java 改动请在 `proxy/` 下通过 `mvn -q test` 或本地启动验证（视改动范围）

## 行为准则

请保持尊重与建设性沟通；技术讨论对事不对人。
