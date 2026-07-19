---
layout: default
title: Chinese
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP孵化器](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![执照](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI与Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![公关欢迎](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker拉取次数](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## 破解它。扫描它。复现它。用它进行基准测试。改进它。

OWASP VulnerableApp 是一个模块化的、刻意包含漏洞的应用，主要用于通过可复现的测试场景验证安全扫描器并对其进行基准测试，同时也支持学习和实验。

### 🔍 它有什么不同

与传统的易受攻击应用不同，VulnerableApp 被设计为一个可测试的安全生态系统，而不是静态的训练应用。

### 它支持：

- 🔧 对 Burp Suite、OWASP ZAP 和自定义 DAST 引擎等工具进行扫描器基准测试
- 🧩 模块化漏洞设计，无需修改核心服务即可添加新场景
- 📊 跨版本和环境的安全回归测试
- 🎯 针对现代 Web 应用模式的真实攻击面模拟
- 🔁 确定性的漏洞行为，以获得可重复的扫描结果
- 🧑‍💻 面向安全工程师、研究人员和教育工作者构建

![完整架构栈](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)

### VulnerableApp 可以帮助您：

- 验证安全工具面对已知漏洞模式时的行为
- 为安全实验构建受控环境
- 随新攻击技术的出现扩展漏洞覆盖范围
- 运行一致、可重复的安全测试流水线

### ⚙️ 为什么它很重要

大多数易受攻击应用都是：
- 静态的
- 难以扩展的
- 仅为手动学习而设计的

### VulnerableApp 的设计目标：

自动化、可复现性与持续演进

### 用户界面

![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## 使用现代 UI 进行测试

VulnerableApp-facade 为 VulnerableApp 提供现代 UI。要使用现代 UI 测试本地更改：

1. **先决条件**：确保已安装 Docker 和 Docker Compose。
2. **运行测试脚本**：
   - Windows：`.\scripts\testWithModernUI.bat`
   - Linux/Mac：`./scripts/testWithModernUI.sh`

该脚本会将本地更改构建为 Docker 镜像 (`sasanlabs/owasp-vulnerableapp:unreleased`)，并使用 `docker-compose.local.yml` 启动完整技术栈（包括 facade、jsp 和 php 服务）。

3. **访问 UI**：打开 `http://localhost`。
4. **访问 Mailpit**：打开 `http://localhost:8025` 查看本地 SMTP 服务器捕获的邮件。

## 使用的技术

- Java 17
- Spring Boot
- ReactJS
- Javascript/TypeScript

## 当前支持的漏洞类型

1. [JWT 漏洞](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [命令注入](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [密码学漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [文件上传漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [路径遍历漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL 注入](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [基于错误的 SQL 注入](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [基于联合查询的 SQL 注入](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [盲注（Blind SQLi）](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [持久型 XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [反射型 XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [开放重定向](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection)
    1. [基于 HTTP 3xx 状态码](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)
12. [点击劫持](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/clickjacking)
13. [LDAP 注入](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ldapInjection)
14. [身份验证漏洞](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/authentication)

## 为项目做贡献

您可以通过以下多种方式为本项目做出贡献：
1. 如果您是开发人员且刚刚开始接触本项目，建议浏览标记有 `good first issue` 的[问题列表](https://github.com/SasanLabs/VulnerableApp/issues)，这些任务是很好的起点。
2. 如果您是开发人员或安全专业人员，希望添加新的漏洞类型，可以运行 `./gradlew GenerateSampleVulnerability` 命令来生成漏洞模板。该模板包含占位符和注释，修改后的文件会在命令日志或 git 历史中列出。您只需导航到对应文件，填写占位符内容，然后构建项目即可查看效果。
3. 如果您希望通过宣传或推动项目发展来做出贡献，欢迎在讨论区或问题列表中分享您的想法，我们非常乐意与您进行交流。

## 运行项目

运行项目有两种方式：
1. 最简便的方式是使用 Docker 容器，这将运行包含所有组件的完整 VulnerableApp：
    1. 下载并安装 [Docker Compose](https://docs.docker.com/compose/install/)
    2. 克隆本 GitHub 仓库
    3. 打开终端并导航到项目根目录
    4. 执行命令 ```docker-compose pull && docker-compose up```
    5. 在浏览器访问 `http://localhost`，即可看到 VulnerableApp 的用户界面。

    **注意**：以上步骤将运行最新的未发布版本。如需运行最新正式版本，请使用 Docker **latest** 标签。

2. 另一种方式是将 VulnerableApp 作为独立应用程序运行：
    1. 前往 GitHub 的[发布页面](https://github.com/SasanLabs/VulnerableApp/releases)，下载最新版本的 JAR 文件
    2. 打开终端并导航到项目根目录
    3. 执行命令 ```java -jar VulnerableApp-*```
    4. 在浏览器访问 `http://localhost:9090/VulnerableApp`，即可看到 VulnerableApp 的旧版用户界面。

## 构建项目

本项目有两种构建方式：
1. 构建为 Docker 应用程序，以运行完整的 VulnerableApp：
    1. 执行 `./gradlew jibDockerBuild` 构建 Docker 镜像
    2. 下载 [Docker-Compose 文件](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) 并在同目录下执行 `docker-compose up`
    3. 在浏览器访问 `http://localhost`，即可看到 VulnerableApp 的用户界面。
2. 构建为 SpringBoot 应用程序，使用旧版 UI 或 REST API，便于调试：
    1. 将项目导入您偏好的 IDE 并运行
    2. 在浏览器访问 `http://localhost:9090/VulnerableApp`，即可通过旧版用户界面进行调试和测试。

### 连接到内嵌的 H2 数据库

通过浏览器访问数据库，请访问：`http://localhost:9090/VulnerableApp/h2`

数据库连接配置：
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## 对扫描器进行基准测试

VulnerableApp 包含一个比较器，可将扫描器的发现与项目内置的标准答案进行比较，并生成覆盖、遗漏和未匹配项报告。DAST 和 SAST 扫描器使用同一端点：

- 端点：`POST http://<baseurl>/VulnerableApp/scanner/benchmark`
- 请求体：DAST 使用 `{ tool, scanType: "DAST", findings: [ { url, type, cwe, wascId } ] }`；SAST 使用 `{ tool, scanType: "SAST", findings: [ { filePath, line, cwe, type } ] }`
- 响应体及磁盘上的 `benchmarks/<tool>-results.json`：覆盖率报告

扫描器本身的运行不在范围内，您需要提供 JSON。完整架构、匹配规则和 `curl` 示例请参阅 [`benchmarks/README.md`](../../../benchmarks/README.md)。

## 联系方式

如果您在任何步骤中遇到困难，或对项目及其目标有任何疑问，欢迎发送邮件至 karan.sasan@owasp.org，或创建一个 [Issue](https://github.com/SasanLabs/VulnerableApp/issues)，我们将尽力为您提供帮助。

## 文档与参考资料

1. [项目文档](https://sasanlabs.github.io/VulnerableApp)
2. [设计文档](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp 主页](https://owasp.org/www-project-vulnerableapp/)
4. [OWASP Spotlight 系列概览视频](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [概览视频](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### 博客文章

1. [OWASP-VulnerableApp 概述 — Medium 文章](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [OWASP-VulnerableApp 概述 — Blogspot 文章](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Kenji Nakajima 对 OWASP VulnerableApp 的介绍](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [基于生成式 AI 的平台 Shannon 利用 VulnerableApp](https://qiita.com/fiord/items/9351bcff6d646862f181)
5. [我构建了 OWASP ZAP 文件上传插件：为什么首先需要 VulnerableApp-Facade](https://medium.com/p/52c4f2226ed3)

### OWASP VulnerableApp 的使用情况

1. [查看全球学术兴趣](../../Usage.md)

### 故障排除参考

1. [Reddit：利用 SQL 注入漏洞](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### 其他语言版本 README

1. [俄语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [印地语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
3. [旁遮普语](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
