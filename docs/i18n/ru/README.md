---
layout: default
title: Russian
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

Поскольку в наши дни веб-приложения становятся всё более популярными, возникает острая необходимость в обеспечении их безопасности. Существует множество инструментов сканирования уязвимостей, однако при их разработке программисты вынуждены тестировать эти инструменты. Кроме того, им необходимо оценивать качество работы инструмента сканирования. На сегодняшний день существует крайне мало уязвимых приложений, пригодных для тестирования таких инструментов. На рынке присутствуют намеренно уязвимые приложения, но они создавались без подобного умысла и потому страдают отсутствием расширяемости — добавление новых уязвимостей в них весьма затруднено.

Как следствие, разработчики вынуждены писать собственные уязвимые приложения, что приводит к потере производительности и многократному выполнению одной и той же работы.

**VulnerableApp** создан с учётом всех этих факторов. Проект является масштабируемым, расширяемым, простым в интеграции и освоении.
Поскольку решение вышеуказанной проблемы требует добавления различных типов уязвимостей, этот проект становится отличной платформой для изучения уязвимостей безопасности.

### Пользовательский интерфейс

![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## Используемые технологии

- Java 17
- Spring Boot
- ReactJS
- Javascript/TypeScript

## Текущий перечень обрабатываемых типов уязвимостей

1. [Уязвимость JWT](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [Внедрение команд (Command Injection)](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [Криптографические уязвимости](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [Уязвимость загрузки файлов](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [Уязвимость обхода пути](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL-инъекция](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [SQL-инъекция на основе ошибок](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [SQL-инъекция на основе UNION](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [Слепая SQL-инъекция](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [Постоянный XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [Отражённый XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [Открытое перенаправление](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection)
    1. [На основе HTTP-кода состояния 3xx](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)

## Участие в проекте

Существует несколько способов внести вклад в проект:
1. Если вы разработчик и только начинаете работу с проектом, рекомендуется ознакомиться со списком [задач](https://github.com/SasanLabs/VulnerableApp/issues) с меткой `good first issue` — они станут хорошей отправной точкой.
2. Если вы разработчик или специалист по безопасности и хотите добавить новый тип уязвимости, запустите команду `./gradlew GenerateSampleVulnerability`. Она создаст шаблон с заглушками и комментариями. Изменённые файлы будут указаны в логах команды или в истории git. Перейдите к этим файлам, заполните заглушки и соберите проект, чтобы увидеть результат.
3. Если вы хотите способствовать росту проекта или его популяризации, не стесняйтесь добавлять свои мысли в раздел обсуждений или в задачи — мы будем рады обсудить их.

## Запуск проекта

Существует два способа запустить проект:
1. Самый простой способ — использовать Docker-контейнеры, которые запустят полноценное VulnerableApp со всеми компонентами:
    1. Загрузите и установите [Docker Compose](https://docs.docker.com/compose/install/)
    2. Клонируйте данный репозиторий GitHub
    3. Откройте терминал и перейдите в корневой каталог проекта
    4. Выполните команду ```docker-compose pull && docker-compose up```
    5. Откройте браузер и перейдите по адресу `http://localhost` — откроется пользовательский интерфейс VulnerableApp.

    **Примечание**: Описанные выше шаги запустят последнюю нерелизную версию VulnerableApp. Для запуска последней стабильной версии используйте тег Docker **latest**.

2. Другой способ — запустить VulnerableApp как самостоятельное приложение:
    1. Перейдите в [раздел релизов](https://github.com/SasanLabs/VulnerableApp/releases) на GitHub и загрузите JAR-файл последней версии
    2. Откройте терминал и перейдите в корневой каталог проекта
    3. Выполните команду ```java -jar VulnerableApp-*```
    4. Откройте браузер и перейдите по адресу `http://localhost:9090/VulnerableApp` — откроется устаревший пользовательский интерфейс VulnerableApp.

## Сборка проекта

Проект можно собрать двумя способами:
1. Как Docker-приложение для запуска полноценного VulnerableApp:
    1. Соберите Docker-образ командой `./gradlew jibDockerBuild`
    2. Загрузите [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) и выполните в той же директории команду `docker-compose up`
    3. Откройте браузер и перейдите по адресу `http://localhost` — откроется пользовательский интерфейс VulnerableApp.
2. Как приложение SpringBoot с Legacy UI или REST API — это удобно для отладки:
    1. Импортируйте проект в вашу IDE и запустите его
    2. Откройте браузер и перейдите по адресу `http://localhost:9090/VulnerableApp` — откроется устаревший пользовательский интерфейс VulnerableApp для отладки и тестирования.

### Подключение к встроенной базе данных H2

Для доступа к базе данных через браузер перейдите по адресу: `http://localhost:9090/VulnerableApp/h2`

Параметры подключения к базе данных:
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## Контакты

Если у вас возникли затруднения с какими-либо шагами или с пониманием целей и устройства проекта, напишите на karan.sasan@owasp.org или создайте [задачу](https://github.com/SasanLabs/VulnerableApp/issues) — мы постараемся помочь.

## Документация и ссылки

1. [Документация](https://sasanlabs.github.io/VulnerableApp)
2. [Описание архитектуры](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [Обзорное видео для серии OWASP Spotlight](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [Обзорное видео](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### Статьи и блоги

1. [Обзор OWASP-VulnerableApp — статья на Medium](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [Обзор OWASP-VulnerableApp — пост в Blogspot](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Введение в OWASP VulnerableApp от Кэндзи Накадзимы](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [Платформа Shannon на базе генеративного ИИ, использующая VulnerableApp](https://qiita.com/fiord/items/9351bcff6d646862f181)

### Ссылки для устранения неполадок

1. [Reddit: использование уязвимости SQL-инъекции](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### README на других языках

1. [Китайский](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
2. [Хинди](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
3. [Пенджабский](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
