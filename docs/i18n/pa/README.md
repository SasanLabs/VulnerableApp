---
layout: default
title: Punjabi
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

ਜਿਵੇਂ ਕਿ ਅੱਜ ਕੱਲ੍ਹ ਵੈੱਬ ਐਪਲੀਕੇਸ਼ਨਾਂ ਤੇਜ਼ੀ ਨਾਲ ਪ੍ਰਸਿੱਧ ਹੋ ਰਹੀਆਂ ਹਨ, ਉਨ੍ਹਾਂ ਨੂੰ ਸੁਰੱਖਿਅਤ ਕਰਨ ਦੀ ਅਤਿਅੰਤ ਜ਼ਰੂਰਤ ਵੀ ਮਹਿਸੂਸ ਹੋ ਰਹੀ ਹੈ। ਭਾਵੇਂ ਕਈ ਕਮਜ਼ੋਰੀ ਸਕੈਨਿੰਗ ਟੂਲ ਮੌਜੂਦ ਹਨ, ਪਰ ਇਹਨਾਂ ਟੂਲਾਂ ਨੂੰ ਵਿਕਸਿਤ ਕਰਦੇ ਸਮੇਂ ਡਿਵੈਲਪਰਾਂ ਨੂੰ ਇਹਨਾਂ ਦੀ ਜਾਂਚ ਕਰਨੀ ਪੈਂਦੀ ਹੈ ਅਤੇ ਇਹ ਵੀ ਜਾਣਨਾ ਪੈਂਦਾ ਹੈ ਕਿ ਟੂਲ ਕਿੰਨਾ ਚੰਗਾ ਕੰਮ ਕਰ ਰਿਹਾ ਹੈ। ਫਿਲਹਾਲ ਅਜਿਹੀਆਂ ਬਹੁਤ ਘੱਟ ਕਮਜ਼ੋਰ ਐਪਲੀਕੇਸ਼ਨਾਂ ਮੌਜੂਦ ਹਨ ਜਿਨ੍ਹਾਂ 'ਤੇ ਇਹਨਾਂ ਟੂਲਾਂ ਦੀ ਜਾਂਚ ਕੀਤੀ ਜਾ ਸਕੇ। ਮਾਰਕੀਟ ਵਿੱਚ ਜਾਣਬੁੱਝ ਕੇ ਕਮਜ਼ੋਰ ਐਪਲੀਕੇਸ਼ਨਾਂ ਮੌਜੂਦ ਹਨ, ਪਰ ਉਹ ਇਸ ਮਕਸਦ ਨਾਲ ਨਹੀਂ ਲਿਖੀਆਂ ਗਈਆਂ ਸਨ ਅਤੇ ਇਸ ਲਈ ਐਕਸਟੈਂਸੀਬਿਲਟੀ ਦੀ ਘਾਟ ਹੈ।

**VulnerableApp** ਇਹਨਾਂ ਸਾਰੇ ਕਾਰਕਾਂ ਨੂੰ ਧਿਆਨ ਵਿੱਚ ਰੱਖ ਕੇ ਬਣਾਇਆ ਗਿਆ ਹੈ। ਇਹ ਪ੍ਰੋਜੈਕਟ ਸਕੇਲੇਬਲ, ਐਕਸਟੈਂਸੀਬਲ, ਏਕੀਕ੍ਰਿਤ ਕਰਨ ਵਿੱਚ ਆਸਾਨ ਅਤੇ ਸਿੱਖਣ ਵਿੱਚ ਸਰਲ ਹੈ।

### ਯੂਜ਼ਰ ਇੰਟਰਫੇਸ

![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## ਵਰਤੀਆਂ ਗਈਆਂ ਤਕਨੀਕਾਂ

- Java 17
- Spring Boot
- ReactJS
- Javascript/TypeScript

## ਵਰਤਮਾਨ ਵਿੱਚ ਸਮਰਥਿਤ ਕਮਜ਼ੋਰੀ ਕਿਸਮਾਂ

1. [JWT ਕਮਜ਼ੋਰੀ](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [ਕਮਾਂਡ ਇੰਜੈਕਸ਼ਨ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [ਕ੍ਰਿਪਟੋਗ੍ਰਾਫ਼ੀ ਅਸਫਲਤਾਵਾਂ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [ਫਾਈਲ ਅਪਲੋਡ ਕਮਜ਼ੋਰੀ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [ਪਾਥ ਟ੍ਰੈਵਰਸਲ ਕਮਜ਼ੋਰੀ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL ਇੰਜੈਕਸ਼ਨ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [ਐਰਰ-ਬੇਸਡ SQL ਇੰਜੈਕਸ਼ਨ](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [ਯੂਨੀਅਨ-ਬੇਸਡ SQL ਇੰਜੈਕਸ਼ਨ](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [ਬਲਾਇੰਡ SQL ਇੰਜੈਕਸ਼ਨ](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [ਪਰਸਿਸਟੈਂਟ XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [ਰਿਫਲੈਕਟਡ XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [ਓਪਨ ਰਿਡਾਇਰੈਕਟ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection)
    1. [HTTP 3xx ਸਟੇਟਸ ਕੋਡ ਆਧਾਰਿਤ](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)

## ਪ੍ਰੋਜੈਕਟ ਲਈ ਯੋਗਦਾਨ

ਤੁਸੀਂ ਹੇਠ ਲਿਖੇ ਤਰੀਕਿਆਂ ਨਾਲ ਪ੍ਰੋਜੈਕਟ ਵਿੱਚ ਯੋਗਦਾਨ ਦੇ ਸਕਦੇ ਹੋ:
1. ਜੇ ਤੁਸੀਂ ਡਿਵੈਲਪਰ ਹੋ ਅਤੇ ਹੁਣੇ ਸ਼ੁਰੂਆਤ ਕਰ ਰਹੇ ਹੋ, ਤਾਂ [ਇਸ਼ੂਜ਼](https://github.com/SasanLabs/VulnerableApp/issues) ਦੀ ਸੂਚੀ ਵੇਖੋ ਜਿਨ੍ਹਾਂ ਵਿੱਚ `good first issue` ਟੈਗ ਹੈ।
2. ਜੇ ਤੁਸੀਂ ਨਵੀਂ ਕਮਜ਼ੋਰੀ ਕਿਸਮ ਜੋੜਨਾ ਚਾਹੁੰਦੇ ਹੋ, ਤਾਂ `./gradlew GenerateSampleVulnerability` ਕਮਾਂਡ ਚਲਾਓ। ਇਹ ਇੱਕ ਸੈਂਪਲ ਟੈਂਪਲੇਟ ਬਣਾਵੇਗਾ — ਉਹ ਫਾਈਲਾਂ ਖੋਲ੍ਹੋ, ਪਲੇਸਹੋਲਡਰ ਭਰੋ ਅਤੇ ਪ੍ਰੋਜੈਕਟ ਬਿਲਡ ਕਰੋ।
3. ਜੇ ਤੁਸੀਂ ਪ੍ਰਚਾਰ ਜਾਂ ਵਿਕਾਸ ਵਿੱਚ ਯੋਗਦਾਨ ਦੇਣਾ ਚਾਹੁੰਦੇ ਹੋ, ਤਾਂ ਡਿਸਕਸ਼ਨ ਸੈਕਸ਼ਨ ਵਿੱਚ ਆਪਣੇ ਵਿਚਾਰ ਸਾਂਝੇ ਕਰੋ।

## ਪ੍ਰੋਜੈਕਟ ਚਲਾਉਣਾ

ਪ੍ਰੋਜੈਕਟ ਚਲਾਉਣ ਦੇ ਦੋ ਤਰੀਕੇ ਹਨ:
1. Docker ਕੰਟੇਨਰਾਂ ਦੀ ਵਰਤੋਂ ਕਰਕੇ (ਸਾਰੇ ਹਿੱਸਿਆਂ ਸਮੇਤ ਪੂਰਾ VulnerableApp):
    1. [Docker Compose](https://docs.docker.com/compose/install/) ਡਾਊਨਲੋਡ ਅਤੇ ਇੰਸਟਾਲ ਕਰੋ
    2. ਇਸ GitHub ਰਿਪੋਜ਼ਟਰੀ ਨੂੰ ਕਲੋਨ ਕਰੋ
    3. ਟਰਮੀਨਲ ਖੋਲ੍ਹੋ ਅਤੇ ਪ੍ਰੋਜੈਕਟ ਦੀ ਰੂਟ ਡਾਇਰੈਕਟਰੀ 'ਤੇ ਜਾਓ
    4. ਕਮਾਂਡ ```docker-compose pull && docker-compose up``` ਚਲਾਓ
    5. ਬ੍ਰਾਊਜ਼ਰ ਵਿੱਚ `http://localhost` 'ਤੇ ਜਾਓ।

    **ਨੋਟ**: ਨਵੀਨਤਮ ਰਿਲੀਜ਼ਡ ਵਰਜ਼ਨ ਲਈ Docker **latest** ਟੈਗ ਵਰਤੋ।

2. ਸਟੈਂਡਅਲੋਨ ਐਪਲੀਕੇਸ਼ਨ ਵਜੋਂ:
    1. GitHub ਦੇ [ਰਿਲੀਜ਼ ਸੈਕਸ਼ਨ](https://github.com/SasanLabs/VulnerableApp/releases) ਤੋਂ ਨਵੀਨਤਮ JAR ਫਾਈਲ ਡਾਊਨਲੋਡ ਕਰੋ
    2. ਕਮਾਂਡ ```java -jar VulnerableApp-*``` ਚਲਾਓ
    3. ਬ੍ਰਾਊਜ਼ਰ ਵਿੱਚ `http://localhost:9090/VulnerableApp` 'ਤੇ ਜਾਓ।

## ਪ੍ਰੋਜੈਕਟ ਬਿਲਡ ਕਰਨਾ

1. Docker ਐਪਲੀਕੇਸ਼ਨ ਵਜੋਂ:
    1. `./gradlew jibDockerBuild` ਨਾਲ Docker ਇਮੇਜ ਬਣਾਓ
    2. [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) ਡਾਊਨਲੋਡ ਕਰੋ ਅਤੇ `docker-compose up` ਚਲਾਓ
    3. ਬ੍ਰਾਊਜ਼ਰ ਵਿੱਚ `http://localhost` 'ਤੇ ਜਾਓ।
2. SpringBoot ਐਪਲੀਕੇਸ਼ਨ ਵਜੋਂ (ਡੀਬੱਗਿੰਗ ਲਈ):
    1. ਪ੍ਰੋਜੈਕਟ ਨੂੰ ਆਪਣੀ IDE ਵਿੱਚ ਇਮਪੋਰਟ ਕਰੋ ਅਤੇ ਚਲਾਓ
    2. ਬ੍ਰਾਊਜ਼ਰ ਵਿੱਚ `http://localhost:9090/VulnerableApp` 'ਤੇ ਜਾਓ।

### ਏਮਬੈੱਡਡ H2 ਡੇਟਾਬੇਸ ਨਾਲ ਕੁਨੈਕਟ ਕਰਨਾ

ਬ੍ਰਾਊਜ਼ਰ ਤੋਂ ਡੇਟਾਬੇਸ ਐਕਸੈੱਸ ਕਰਨ ਲਈ: `http://localhost:9090/VulnerableApp/h2`

```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## ਸੰਪਰਕ

ਕਿਸੇ ਵੀ ਮੁੱਦੇ ਲਈ karan.sasan@owasp.org 'ਤੇ ਈਮੇਲ ਕਰੋ ਜਾਂ ਇੱਕ [ਇਸ਼ੂ](https://github.com/SasanLabs/VulnerableApp/issues) ਉਠਾਓ।

## ਦਸਤਾਵੇਜ਼ਾਂ ਅਤੇ ਹਵਾਲੇ

1. [ਦਸਤਾਵੇਜ਼ੀਕਰਨ](https://sasanlabs.github.io/VulnerableApp)
2. [ਡਿਜ਼ਾਈਨ ਦਸਤਾਵੇਜ਼ੀਕਰਨ](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [OWASP Spotlight ਲੜੀ ਦਾ ਸੰਖੇਪ ਵੀਡੀਓ](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [ਸੰਖੇਪ ਵੀਡੀਓ](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### ਬਲੌਗ

1. [OWASP-VulnerableApp ਦਾ ਸੰਖੇਪ — Medium ਲੇਖ](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [OWASP-VulnerableApp ਦਾ ਸੰਖੇਪ — Blogspot ਪੋਸਟ](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Kenji Nakajima ਦੁਆਰਾ OWASP VulnerableApp ਦੀ ਜਾਣ-ਪਛਾਣ](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [ਜੈਨਰੇਟਿਵ AI ਆਧਾਰਿਤ ਪਲੇਟਫਾਰਮ Shannon](https://qiita.com/fiord/items/9351bcff6d646862f181)

### ਸਮੱਸਿਆ ਨਿਵਾਰਣ ਹਵਾਲੇ

1. [Reddit: SQL ਇੰਜੈਕਸ਼ਨ ਕਮਜ਼ੋਰੀ ਦੀ ਵਰਤੋਂ](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### ਹੋਰ ਭਾਸ਼ਾਵਾਂ ਵਿੱਚ README

1. [ਰੂਸੀ](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [ਚੀਨੀ](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [ਹਿੰਦੀ](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
