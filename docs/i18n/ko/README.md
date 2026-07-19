---
layout: default
title: Korean
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## 부수고, 스캔하고, 재현하고, 벤치마크하고, 개선하라.

OWASP VulnerableApp은 재현 가능한 테스트 시나리오를 통해 보안 스캐너를 검증하고 벤치마킹하는 것을 주목적으로 설계된, 의도적으로 취약하게 만든 모듈형 애플리케이션입니다. 동시에 학습과 실험 용도로도 활용할 수 있습니다.

### 🔍 무엇이 다른가
기존의 취약한 애플리케이션들과 달리, VulnerableApp은 정적인 학습용 앱이 아니라 테스트 가능한 보안 생태계로 설계되었습니다.

### 이 프로젝트로 할 수 있는 것:

- 🔬 Burp Suite, OWASP ZAP, 커스텀 DAST 엔진 등 다양한 도구에 대한 스캐너 벤치마킹
- 🧩 핵심 서비스를 건드리지 않고도 새로운 시나리오를 추가할 수 있는 모듈형 취약점 설계
- 📊 릴리스와 환경 전반에 걸친 보안 회귀 테스트
- 🎯 현대 웹 애플리케이션 패턴을 반영한 현실적인 공격 표면 시뮬레이션
- 🧪 반복 가능한 스캐닝 결과를 위한 결정론적(deterministic) 취약점 동작
- 🧠 보안 엔지니어, 연구자, 교육자를 위해 만들어짐

![Entire architecture stack](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)


### VulnerableApp이 도와줄 수 있는 것:

- 알려진 취약점 패턴에 대해 보안 도구가 어떻게 동작하는지 검증
- 보안 실험을 위한 통제된 환경 구축
- 새로운 공격 기법이 등장할 때마다 취약점 커버리지 확장
- 일관되고 반복 가능한 보안 테스트 파이프라인 실행

### ⚙️ 왜 중요한가

대부분의 취약한 애플리케이션은:
- 정적이고
- 확장하기 어려우며
- 수동 학습만을 위해 설계되어 있습니다

### VulnerableApp이 지향하는 것:
자동화, 재현성, 그리고 진화

### 사용자 인터페이스 ###
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## 프로젝트 실행하기

프로젝트를 실행하는 방법은 두 가지가 있습니다:
1. 가장 간단한 방법은 Docker 컨테이너를 사용하는 것으로, 모든 구성 요소가 포함된 완전한 VulnerableApp을 실행합니다:
    1. [Docker Compose](https://docs.docker.com/compose/install/)를 다운로드하여 설치합니다
    2. 이 GitHub 저장소를 클론합니다
    3. 터미널을 열고 프로젝트 루트 디렉터리로 이동합니다
    4. ```docker-compose pull && docker-compose up``` 명령을 실행합니다
    5. 브라우저를 열고 `http://localhost`로 이동하면 VulnerableApp 사용자 인터페이스가 표시됩니다.
    6. 로컬 SMTP 서버가 캡처한 이메일을 확인하려면 `http://localhost/mailpit/`에서 Mailpit도 이용할 수 있습니다.

    **참고**: 위의 단계는 최신 미출시(non-release) 버전의 VulnerableApp을 실행합니다. 최신 안정 버전을 실행하려면 Docker의 **latest** 태그를 사용하세요.

2. 다른 방법은 VulnerableApp을 독립 실행형(standalone) 애플리케이션으로 실행하는 것입니다:
    1. GitHub의 [릴리스 섹션](https://github.com/SasanLabs/VulnerableApp/releases)으로 이동하여 최신 버전의 JAR 파일을 다운로드합니다
    2. 터미널을 열고 프로젝트 루트 디렉터리로 이동합니다
    3. ```java -jar VulnerableApp-*``` 명령을 실행합니다
    4. 브라우저를 열고 `http://localhost:9090/VulnerableApp`으로 이동하면 레거시(Legacy) 사용자 인터페이스가 표시됩니다.

## 프로젝트 빌드하기

프로젝트는 두 가지 방식으로 빌드할 수 있습니다:
1. 완전한 VulnerableApp을 실행하기 위한 Docker 애플리케이션으로 빌드하기:
    1. `./gradlew jibDockerBuild` 명령으로 Docker 이미지를 빌드합니다
    2. [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml)를 다운로드하고 같은 디렉터리에서 `docker-compose up`을 실행합니다
    3. 브라우저를 열고 `http://localhost`로 이동하면 VulnerableApp 사용자 인터페이스가 표시됩니다.
2. 레거시 UI 또는 REST API를 사용하는 SpringBoot 애플리케이션으로 빌드하기 — 디버깅에 유용합니다:
    1. 프로젝트를 IDE로 가져와서 실행합니다
    2. 브라우저를 열고 `http://localhost:9090/VulnerableApp`으로 이동하면 디버깅 및 테스트를 위한 레거시 VulnerableApp 사용자 인터페이스가 표시됩니다.

## 프로젝트 기여하기

프로젝트에 기여하는 방법은 여러 가지가 있습니다:
1. 개발자이며 프로젝트를 이제 막 시작하는 단계라면, `good first issue` 라벨이 붙은 [이슈](https://github.com/SasanLabs/VulnerableApp/issues) 목록을 확인해보는 것을 권장합니다. 시작하기 좋은 항목들입니다.
2. 개발자이거나 보안 전문가로서 새로운 유형의 취약점을 추가하고 싶다면, `./gradlew GenerateSampleVulnerability` 명령을 실행하세요. 이 명령은 자리표시자(placeholder)와 주석이 포함된 템플릿을 생성합니다. 변경된 파일은 명령 실행 로그나 git 히스토리에서 확인할 수 있습니다. 해당 파일로 이동해 자리표시자를 채우고 프로젝트를 빌드하여 결과를 확인하세요.
3. 프로젝트의 성장이나 홍보에 기여하고 싶다면, 토론(discussion)이나 이슈 섹션에 자유롭게 의견을 남겨주세요. 기꺼이 논의하겠습니다.

## 모던 UI로 테스트하기
VulnerableApp-facade는 VulnerableApp을 위한 모던 UI를 제공합니다. 로컬에서 변경한 내용을 모던 UI로 테스트하려면:

1. **사전 준비**: Docker와 Docker-Compose가 설치되어 있어야 합니다.
2. **테스트 스크립트 실행**:
   - Windows: `.\scripts\testWithModernUI.bat`
   - Linux/Mac: `./scripts/testWithModernUI.sh`

이 스크립트는 로컬에서 변경한 내용을 Docker 이미지(`sasanlabs/owasp-vulnerableapp:unreleased`)로 빌드하고, `docker-compose.local.yml`을 사용해 facade, jsp, php 서비스를 포함한 전체 스택을 실행합니다.

3. **UI 접속**: 브라우저에서 `http://localhost`로 이동하면 변경된 내용이 반영된 모던 UI를 확인할 수 있습니다.

4. **Mailpit 접속**: 브라우저에서 `http://localhost/mailpit/`로 이동하면 로컬 SMTP 서버가 캡처한 이메일을 확인할 수 있습니다.

## 사용된 기술

- Java 17
- Spring Boot
- ReactJS
- Javascript/TypeScript

### 내장 H2 데이터베이스 연결하기

브라우저를 통해 데이터베이스에 접근하려면 다음 주소로 이동하세요: `http://localhost:9090/VulnerableApp/h2`

데이터베이스 연결 정보:
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## 현재 다루고 있는 취약점 목록

1. [JWT 취약점](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [커맨드 인젝션](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [암호화 관련 취약점](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [파일 업로드 취약점](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [경로 탐색(Path Traversal) 취약점](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL 인젝션](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [에러 기반 SQL 인젝션](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [UNION 기반 SQL 인젝션](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [블라인드 SQL 인젝션](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [저장형(Persistent) XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [반사형(Reflected) XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [오픈 리다이렉트](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/openRedirect)
    1. [HTTP 3xx 상태 코드 기반 인젝션](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)
12. [클릭재킹(Clickjacking)](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/clickjacking)
13. [LDAP 인젝션](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ldapInjection)
14. [인증 취약점](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/authentication)

## 스캐너 벤치마킹하기

VulnerableApp은 스캐너가 찾아낸 결과를 프로젝트에 내장된 정답(ground truth)과 비교해서 채점하고, 커버리지/누락/불일치 리포트를 만들어주는 비교기(comparator)를 제공합니다. DAST와 SAST 스캐너 모두 동일한 엔드포인트로 지원됩니다:

- 엔드포인트: `POST http://<baseurl>/VulnerableApp/scanner/benchmark`
- 요청 본문 — 사용하는 스캐너에 맞는 형태를 선택하세요:
  - DAST: `{ tool, scanType: "DAST", findings: [ { url, type, cwe, wascId } ] }` (`scanType`은 선택 항목이며 기본값은 `DAST`입니다. `type`/`cwe`/`wascId`도 각각 선택 항목이며, 이 중 하나만 일치해도 충분합니다)
  - SAST: `{ tool, scanType: "SAST", findings: [ { filePath, line, cwe, type } ] }`
- 응답 본문과 디스크에 저장되는 `benchmarks/<tool>-results.json`: 커버리지 리포트

스캐너 자체를 실행하는 것은 이 기능의 범위 밖이며, 사용자가 JSON을 직접 제공해야 합니다. 전체 입출력 스키마, 매칭 규칙, 표준 취약점 유형 용어집, `curl` 예제는 [`benchmarks/README.md`](../../../benchmarks/README.md)를 참고하세요.

## 문의

진행 단계나 프로젝트의 목적 및 구조를 이해하는 데 어려움이 있다면 karan.sasan@owasp.org로 메일을 보내시거나 [이슈](https://github.com/SasanLabs/VulnerableApp/issues)를 생성해 주세요. 최선을 다해 도와드리겠습니다.

## 문서 및 링크

1. [문서](https://sasanlabs.github.io/VulnerableApp)
2. [설계 문서](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [OWASP Spotlight 시리즈 소개 영상](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [소개 영상](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### 기사 및 블로그

1. [OWASP-VulnerableApp 개요 — Medium 게시글](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [OWASP-VulnerableApp 개요 — Blogspot 게시글](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Kenji Nakajima의 OWASP VulnerableApp 소개](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [VulnerableApp을 활용한 생성형 AI 기반 Shannon 플랫폼](https://qiita.com/fiord/items/9351bcff6d646862f181)
5. [OWASP ZAP 파일 업로드 애드온을 만든 이야기 — VulnerableApp-Facade가 먼저 존재해야 했던 이유](https://medium.com/p/52c4f2226ed3)

### OWASP VulnerableApp 활용 사례
1. [전 세계 학계의 관심 현황 보기](../../Usage.md)

### 트러블슈팅 관련 링크
1. [Reddit: SQL 인젝션 취약점 활용 사례](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### 다른 언어로 된 README

1. [러시아어](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [중국어](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [힌디어](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
4. [펀자브어](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
