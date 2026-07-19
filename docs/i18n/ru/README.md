---
layout: default
title: Russian
parent: Locale
---

# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![Лицензия](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI с Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PR приветствуются](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Загрузки Docker](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## Взломайте. Просканируйте. Воспроизведите. Сравните. Улучшите.

OWASP VulnerableApp — это модульное намеренно уязвимое приложение, разработанное главным образом для проверки и сравнительного тестирования сканеров безопасности с помощью воспроизводимых тестовых сценариев, а также для обучения и экспериментов.

### 🔍 Что делает его особенным
В отличие от традиционных уязвимых приложений, VulnerableApp разработано как тестируемая экосистема безопасности, а не как статическое учебное приложение.

### Оно позволяет:

- 🔬 Проводить сравнительное тестирование сканеров, таких как Burp Suite, OWASP ZAP и пользовательские DAST-движки
- 🧩 Использовать модульную архитектуру уязвимостей, позволяющую добавлять новые сценарии без изменения основных сервисов
- 📊 Выполнять регрессионное тестирование безопасности между релизами и окружениями
- 🎯 Моделировать реалистичную поверхность атаки для современных шаблонов веб-приложений
- 🧪 Обеспечивать детерминированное поведение уязвимостей для повторяемых результатов сканирования
- 🧠 Быть полезным инженерам по безопасности, исследователям и преподавателям

![Полная архитектура](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)

### VulnerableApp помогает вам:

- Проверять, как инструменты безопасности ведут себя на известных шаблонах уязвимостей
- Создавать контролируемые среды для экспериментов в области безопасности
- Расширять покрытие уязвимостей по мере появления новых методов атак
- Запускать согласованные и воспроизводимые конвейеры тестирования безопасности

### ⚙️ Почему это важно

Большинство уязвимых приложений:
- Статичны
- Трудно расширяются
- Предназначены только для ручного обучения

### VulnerableApp создано для:
автоматизации, воспроизводимости и развития

### Пользовательский интерфейс ###
![Интерфейс VulnerableApp-facade](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## Запуск проекта
Существует 2 способа запуска проекта:
1. Самый простой способ — использовать Docker-контейнеры, которые запускают полнофункциональное приложение VulnerableApplication со всеми компонентами. Чтобы запустить приложение через Docker, выполните следующие шаги:
    1. Загрузите и установите [Docker Compose](https://docs.docker.com/compose/install/)
    2. Клонируйте этот репозиторий GitHub
    3. Откройте терминал и перейдите в корневой каталог проекта
    4. Выполните команду ```docker-compose pull && docker-compose up```
    5. Откройте в браузере `http://localhost` — откроется пользовательский интерфейс VulnerableApp.
    6. Mailpit также доступен по адресу `http://localhost:8025` для просмотра писем, перехваченных локальным SMTP-сервером.

    **Примечание**: приведённые выше шаги запустят последнюю ещё не выпущенную версию VulnerableApp. Если вы хотите использовать последнюю официально выпущенную версию, используйте Docker-тег **latest**.
2. Другой способ — запуск VulnerableApp как автономного приложения:
    1. Перейдите в раздел [Releases](https://github.com/SasanLabs/VulnerableApp/releases) на GitHub и загрузите JAR-файл последней выпущенной версии
    2. Откройте терминал и перейдите в корневой каталог проекта
    3. Выполните команду ```java -jar VulnerableApp-*```
    4. Откройте в браузере `http://localhost:9090/VulnerableApp`. Это откроет классический пользовательский интерфейс VulnerableApp.

## Сборка проекта
Существует 2 способа собрать и использовать этот проект:
1. Как Docker-приложение, позволяющее запускать полнофункциональное VulnerableApplication. Для этого:
    1. Соберите Docker-образ командой `./gradlew jibDockerBuild`
    2. Загрузите [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) и выполните в том же каталоге команду `docker-compose up`
    3. Откройте в браузере `http://localhost` — откроется пользовательский интерфейс VulnerableApp.
2. Как Spring Boot-приложение, работающее с классическим интерфейсом или REST API, что позволяет выполнять отладку и устранять проблемы. Это наиболее простой способ:
    1. Импортируйте проект в предпочитаемую IDE и запустите его
    2. Откройте в браузере `http://localhost:9090/VulnerableApp` — откроется классический пользовательский интерфейс VulnerableApp, который можно использовать для отладки и тестирования.

## Участие в проекте

Есть несколько способов внести вклад в проект:
1. Если вы разработчик и только начинаете работать с проектом, рекомендуется ознакомиться со списком [issues](https://github.com/SasanLabs/VulnerableApp/issues), содержащим задачи с пометкой `good first issue`, которые отлично подходят для начала.
2. Если вы разработчик или специалист по безопасности и хотите добавить новый тип уязвимости, выполните `./gradlew GenerateSampleVulnerability`. Будет создан шаблон примерной уязвимости с заполнителями и комментариями. Изменённые файлы будут указаны в журнале выполнения команды или в истории GitHub. Перейдите к этим файлам, заполните необходимые места, затем соберите проект, чтобы увидеть результат изменений.
3. Если вы хотите помочь развитию проекта, рассказывая о нём или участвуя в его продвижении, не стесняйтесь делиться своими идеями в разделе обсуждений или через issues — мы с удовольствием их обсудим.

## Тестирование с современным интерфейсом
VulnerableApp-facade предоставляет современный пользовательский интерфейс для VulnerableApp. Чтобы протестировать локальные изменения с современным интерфейсом:

1. **Предварительное условие**: убедитесь, что у вас установлены Docker и Docker-Compose.
2. **Запустите сценарий тестирования**:
   - В Windows: `.\scripts\testWithModernUI.bat`
   - В Linux/Mac: `./scripts/testWithModernUI.sh`

Этот сценарий соберёт ваши локальные изменения в Docker-образ (`sasanlabs/owasp-vulnerableapp:unreleased`) и запустит полный стек (включая facade, jsp и php-сервисы) с использованием `docker-compose.local.yml`.

3. **Откройте интерфейс**: перейдите по адресу `http://localhost`, чтобы увидеть современный интерфейс с вашими изменениями.

4. **Откройте Mailpit**: перейдите по адресу `http://localhost:8025`, чтобы просмотреть письма, перехваченные локальным SMTP-сервером.

## Используемые технологии
- Java17
- Spring Boot
- ReactJS
- Javascript/TypeScript

### Подключение к встроенной базе данных H2
Для доступа к базе данных из браузера перейдите по адресу: `http://localhost:9090/VulnerableApp/h2`

Параметры подключения к базе данных:
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## Поддерживаемые в настоящее время типы уязвимостей

1. [Уязвимости JWT](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [Внедрение команд (Command Injection)](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [Ошибки криптографии](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [Уязвимость загрузки файлов](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [Уязвимость обхода каталогов (Path Traversal)](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL-инъекция](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [SQLi на основе ошибок](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [SQLi на основе UNION](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [Слепая SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [Постоянная XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [Отражённая XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [Открытое перенаправление (Open Redirect)](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/openRedirect)
    1. [На основе HTTP-кодов состояния 3xx](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)
12. [Clickjacking](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/clickjacking)
13. [LDAP-инъекция](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ldapInjection)
14. [Уязвимости аутентификации](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/authentication)

## Сравнительное тестирование вашего сканера

VulnerableApp включает компаратор, который оценивает результаты работы сканера относительно встроенной «эталонной» базы проекта и создаёт отчёт о покрытии, пропущенных и лишних находках. Один и тот же конечный пункт поддерживает как DAST-, так и SAST-сканеры:

- Конечная точка: `POST http://<baseurl>/VulnerableApp/scanner/benchmark`
- Тело запроса — выберите формат, соответствующий вашему сканеру:
  - DAST: `{ tool, scanType: "DAST", findings: [ { url, type, cwe, wascId } ] }` (`scanType` необязателен и по умолчанию равен `DAST`; поля `type`/`cwe`/`wascId` по отдельности необязательны — достаточно совпадения по любому одному критерию)
  - SAST: `{ tool, scanType: "SAST", findings: [ { filePath, line, cwe, type } ] }`
- Тело ответа и файл `benchmarks/<tool>-results.json` на диске: отчёт о покрытии

Запуск самого сканера не входит в область ответственности проекта — вы самостоятельно предоставляете JSON. См.
[`benchmarks/README.md`](benchmarks/README.md) для получения полной информации о схемах входных и выходных данных, правилах сопоставления, каноническом словаре типов уязвимостей и примерах `curl`.

## Контакты
Если вы столкнулись с трудностями при выполнении какого-либо из шагов или хотите лучше понять проект и его цели, напишите на адрес karan.sasan@owasp.org или создайте [issue](https://github.com/SasanLabs/VulnerableApp/issues), и мы постараемся помочь.

## Документация и ссылки

1. [Документация](https://sasanlabs.github.io/VulnerableApp)
2. [Документация по архитектуре](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [Обзорное видео серии OWASP Spotlight](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [Обзорное видео](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### Блоги
1. [Обзор Owasp-VulnerableApp — статья на Medium](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [Обзор Owasp-VulnerableApp — публикация на Blogspot](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Введение в Owasp VulnerableApp от Kenji Nakajima](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [Платформа Shannon на базе Gen AI, эксплуатирующая VulnerableApp](https://qiita.com/fiord/items/9351bcff6d646862f181)
5. [Я создал дополнение OWASP ZAP File Upload. Почему сначала пришлось создать VulnerableApp-Facade](https://medium.com/p/52c4f2226ed3)

### Использование OWASP VulnerableApp
1. [Посмотреть глобальный академический интерес](./docs/Usage.md)

### Материалы по устранению неполадок
1. [Обсуждение эксплуатации SQL-инъекции в Reddit](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### README на других языках

1. [Русский](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [Китайский](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [Хинди](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
4. [Панджаби](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
5. [Корейский](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ko/README.md)
