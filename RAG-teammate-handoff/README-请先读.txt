================================================================================
  EGS · RAG / 向量检索 队友对接包（从主仓库拷贝，便于整夹压缩发送）
================================================================================

【你怎么发】
  1. 在资源管理器中打开本目录的上一级：  D:\EGS\EGSfb\
  2. 右键文件夹 「RAG-teammate-handoff」→ 发送到 → 压缩(zipped)文件夹
  3. 把生成的 zip 发给队友（或上传网盘）

【本包不含】
  · 真实 API Key（切勿把 application-local.yml 含密钥的版本打进包）
  · 完整工程源码（仅摘了对接最需要的文件）

【目录说明】（主仓库已整理：编号目录已迁入 archive/；本手抄包仍保留原结构便于单夹发送）
  01-docs\          三份 Markdown：密钥说明、向量库现状、文件清单（主仓库见 docs\ 同名文件）
  02-proxy-java\    Java：LlmController（千问调用范例）、QwenProperties
  02-proxy-resources\  application.yml + application-local.yml.example
  03-front-reference\  vite 代理 + RobotAgent.vue（弹窗调 /api 示例）

【队友先看】
  主仓库：docs\发给队友-RAG模块对接-文件清单.md ；本包：01-docs\发给队友-RAG模块对接-文件清单.md

【与主仓库关系】
  主仓库更新后，若需同步最新对接文件，可让维护者在 EGSfb 下重新执行拷贝脚本，
  或直接从下列路径覆盖本包对应文件：
    proxy\src\main\java\com\example\controller\LlmController.java
    proxy\src\main\java\com\example\config\QwenProperties.java
    proxy\src\main\resources\application.yml
    front\vite.config.js
    front\src\components\RobotAgent.vue

================================================================================