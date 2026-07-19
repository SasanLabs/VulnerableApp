---
layout: default
title: Chinese
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP 孵化项目](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![许可证](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![使用 Gradle 的 Java CI](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![欢迎 PR](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker 拉取次数](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## 破解它。扫描它。复现它。以它为基准进行测试。改进它。

OWASP VulnerableApp 是一个模块化的、刻意包含漏洞的应用程序，主要用于通过可复现的测试场景验证和基准测试安全扫描器，同时也支持学习和实验。

### 🔍 它有什么不同
与传统的漏洞应用不同，VulnerableApp 被设计成一个可测试的安全生态系统，而不是一个静态的训练应用。

### 它支持：

- 🔬 为 Burp Suite、OWASP ZAP 以及自定义 DAST 引擎等工具进行扫描器基准测试
- 🧩 模块化漏洞设计，无需修改核心服务即可添加新的漏洞场景
- 📊 跨版本和跨环境的安全回归测试
- 🎯 针对现代 Web 应用模式的真实攻击面模拟
- 🧪 确定性的漏洞行为，确保扫描结果可重复
- 🧠 专为安全工程师、研究人员和教育工作者打造

![完整架构栈](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)

### VulnerableApp 可以帮助你：

- 验证安全工具在已知漏洞模式下的行为
- 构建用于安全实验的可控环境
- 随着新攻击技术的出现扩展漏洞覆盖范围
- 运行一致、可重复的安全测试流水线

### ⚙️ 为什么它很重要

大多数漏洞应用都是：
- 静态的
- 难以扩展
- 仅为手动学习而设计

### VulnerableApp 的设计目标是：
自动化、可复现性和持续演进

### 用户界面 ###
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## 运行项目
有两种方式可以运行该项目：

1. 最简单的方式是使用 Docker 容器，它将运行包含所有组件的完整 VulnerableApplication。使用 Docker 运行时，请按照以下步骤：
    1. 下载并安装 [Docker Compose](https://docs.docker.com/compose/install/)
    2. 克隆此 GitHub 仓库
    3. 打开终端并进入项目根目录
    4. 运行命令：```docker-compose pull && docker-compose up```
    5. 打开浏览器访问 `http://localhost`，即可进入 VulnerableApp 用户界面。
    6. Mailpit 也可通过 `http://localhost:8025` 访问，用于查看本地 SMTP 服务器捕获的电子邮件。

    **注意**：以上步骤运行的是最新的未发布版本。如果希望运行最新正式发布版本，请使用 Docker 的 **latest** 标签。

2. 另一种方式是以独立 Vulnerable Application 的形式运行：
    1. 前往 GitHub 的 [Releases 页面](https://github.com/SasanLabs/VulnerableApp/releases)，下载最新发布版本的 Jar 文件
    2. 打开终端并进入项目根目录
    3. 运行命令：```java -jar VulnerableApp-*```
    4. 打开浏览器访问 `http://localhost:9090/VulnerableApp`，即可进入 VulnerableApp 的传统（Legacy）用户界面。

## 构建项目
该项目有两种构建和使用方式：

1. 构建为 Docker 应用，以运行完整的 VulnerableApplication。步骤如下：
    1. 运行 `./gradlew jibDockerBuild` 构建 Docker 镜像
    2. 下载 [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml)，并在相同目录中运行 `docker-compose up`
    3. 打开浏览器访问 `http://localhost`，即可进入 VulnerableApp 用户界面。

2. 构建为 Spring Boot 应用，它将运行传统 UI 或 REST API，同时便于调试和排查问题。这也是最简单的方法：
    1. 将项目导入你喜欢的 IDE 并运行
    2. 打开浏览器访问：`http://localhost:9090/VulnerableApp`，即可进入 VulnerableApp 的传统用户界面，用于调试和测试。

## 为项目做贡献

你可以通过多种方式参与项目：

1. 如果你是开发者并准备参与项目，建议查看 [issues](https://github.com/SasanLabs/VulnerableApp/issues) 列表，其中带有 `good first issue` 标签的问题非常适合作为入门任务。
2. 如果你是开发者或安全专业人员，希望添加新的漏洞类型，可以运行 `./gradlew GenerateSampleVulnerability` 生成漏洞示例模板。该模板包含占位符和注释。修改后的文件可在命令日志或 GitHub 提交历史中查看。进入这些文件，填写占位内容，然后重新构建项目即可查看修改效果。
3. 如果你希望通过宣传项目或推动项目发展来贡献力量，欢迎在 Discussions 或 Issues 中提出你的想法，我们很乐意一起讨论。

## 使用现代 UI 测试
VulnerableApp-facade 为 VulnerableApp 提供了现代化用户界面。如需使用现代 UI 测试本地修改：

1. **前提条件**：确保已安装 Docker 和 Docker-Compose。
2. **运行测试脚本**：
   - Windows：`.\scripts\testWithModernUI.bat`
   - Linux/Mac：`./scripts/testWithModernUI.sh`

该脚本会将你的本地修改构建为 Docker 镜像（`sasanlabs/owasp-vulnerableapp:unreleased`），并使用 `docker-compose.local.yml` 启动完整的技术栈（包括 facade、jsp 和 php 服务）。

3. **访问 UI**：打开 `http://localhost` 查看包含你修改内容的现代 UI。

4. **访问 Mailpit**：打开 `http://localhost:8025` 查看本地 SMTP 服务器捕获的电子邮件。

## 使用的技术
- Java17
- Spring Boot
- ReactJS
- JavaScript/TypeScript

### 连接到内置 H2 数据库
要在浏览器中访问数据库，请访问：`http://localhost:9090/VulnerableApp/h2`

数据库连接属性：
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## 当前支持的漏洞类型

1. [JWT 漏洞](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [命令注入（Command Injection）](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [密码学失败（Cryptography Failures）](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [文件上传漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [路径遍历漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL 注入](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [基于错误的 SQL 注入](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [基于 UNION 的 SQL 注入](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [盲注（Blind SQLi）](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [持久型 XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [反射型 XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [开放重定向（Open Redirect）](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/openRedirect)
    1. [基于 HTTP 3xx 状态码](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR（不安全直接对象引用）](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)
12. [点击劫持（Clickjacking）](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/clickjacking)
13. [LDAP 注入](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ldapInjection)
14. [身份验证漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/authentication)

## 对你的扫描器进行基准测试

VulnerableApp 内置了一个比较器，可将扫描器发现的问题与项目内置的真实漏洞基准进行对比，并生成覆盖率 / 漏检 / 未匹配报告。DAST 和 SAST 扫描器均支持通过同一个接口进行测试：

- 接口：`POST http://<baseurl>/VulnerableApp/scanner/benchmark`
- 请求体 —— 根据你的扫描器选择相应格式：
  - DAST：`{ tool, scanType: "DAST", findings: [ { url, type, cwe, wascId } ] }`（`scanType` 可选，默认值为 `DAST`；`type`、`cwe`、`wascId` 均为可选字段——任意一个维度匹配即可）
  - SAST：`{ tool, scanType: "SAST", findings: [ { filePath, line, cwe, type } ] }`
- 响应体以及磁盘上的 `benchmarks/<tool>-results.json`：覆盖率报告

扫描器本身的运行不在本项目范围内——你只需提供 JSON。完整的输入/输出 Schema、匹配规则、规范化漏洞类型词汇表以及 `curl` 示例，请参阅：
[`benchmarks/README.md`](benchmarks/README.md)

## 联系方式
如果你在任何步骤中遇到困难，或者对项目及其目标有任何疑问，欢迎发送邮件至 karan.sasan@owasp.org，或者提交一个 [issue](https://github.com/SasanLabs/VulnerableApp/issues)，我们将尽力提供帮助。

## 文档与参考资料

1. [文档](https://sasanlabs.github.io/VulnerableApp)
2. [设计文档](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [OWASP Spotlight 系列概览视频](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [概览视频](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### 博客

1. [OWASP VulnerableApp 概述 - Medium 文章](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [OWASP VulnerableApp 概述 - Blogspot 文章](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Kenji Nakajima 对 OWASP VulnerableApp 的介绍](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [基于生成式 AI 的平台 Shannon 利用 VulnerableApp](https://qiita.com/fiord/items/9351bcff6d646862f181)
5. [我构建了 OWASP ZAP 文件上传插件：为什么必须先有 VulnerableApp-Facade](https://medium.com/p/52c4f2226ed3)

### OWASP VulnerableApp 的使用情况

1. [查看全球学术关注度](./docs/Usage.md)

### 故障排除参考

1. [Reddit：利用 SQL 注入漏洞的讨论](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### 其他语言版本 README

1. [俄语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [中文](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [印地语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
4. [旁遮普语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
5. [韩语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ko/README.md)
