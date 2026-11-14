[English](README.md) | [Русский](docs/i18n/ru/README.md) | [中文](docs/i18n/zh-CN/README.md) | [हिन्दी](docs/i18n/hi/README.md) | [ਪੰਜਾਬੀ](docs/i18n/pu/README.md) | [Français](docs/i18n/fr/README.md) | [Español](docs/i18n/es/README.md) | [Deutsch](docs/i18n/de/README.md) | [日本語](docs/i18n/ja/README.md)

# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/SasanLabs/VulnerableApp/blob/master/LICENSE) [![Docker Pulls](https://img.shields.io/docker/pulls/sasanlabs/owasp-vulnerableapp)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp)

## Break it. Scan it. Reproduce it. Benchmark against it. Improve it.

OWASP VulnerableApp is a modular deliberately vulnerable application designed primarily for validating and benchmarking security scanners through reproducible test scenarios, while also supporting hands-on security education and testing.

### 🔍 What makes it different
Unlike traditional vulnerable applications, VulnerableApp is designed as a testable security ecosystem, not a static training app.

### It enables:

- 🔬 Scanner benchmarking for tools like Burp Suite, OWASP ZAP, and custom DAST engines
- 🧩 Modular vulnerability design that allows new scenarios without modifying core services
- 📊 Security regression testing across releases and environments
- 🎯 Realistic attack surface simulation for modern web application patterns
- 🧪 Deterministic vulnerability behavior for repeatable scanning results
- 🧠 Built for security engineers, researchers, and educators

![Entire architecture stack](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/Sasanlabs/english.png)

### VulnerableApp helps you:

- Validate how security tools behave across known vulnerability patterns
- Build controlled environments for security experimentation
- Extend vulnerability coverage as new attack techniques emerge
- Run consistent, repeatable security testing pipelines

### ⚙️ Why it matters

Most vulnerable apps are:
- Static
- Hard to extend
- Designed only for manual learning

### VulnerableApp is built for:
automation, reproducibility, and evolution

## 🎥 Video Overview

New to the project? Watch this OWASP Spotlight session for a high-level overview of VulnerableApp:

[OWASP Spotlight series: VulnerableApp overview](https://www.youtube.com/watch?v=hoCxzQQugZc&list=PLUKo5k_oSrfOTl27gUmk2o-NBKvkTGw0T)

## User Interface
#### Challenge Mode
![Challenge Mode](docs/gifs/challenge-mode.gif)

#### Switching Modes (Challenge & Scanner)
![Switching Modes](docs/gifs/scanner-mode.gif)

#### Scanner Mode Endpoints (DAST & SAST)
![Scanner Mode](docs/gifs/scanner-mode1.gif)

#### Scanner Mode Benchmark Comparator
![Scanner Benchmark](docs/gifs/scanner-benchmark.gif)

### Running the project

> [!WARNING]
> VulnerableApp is deliberately vulnerable. Run it only on your own machine, in a VM, or in a Docker container, and never expose it to the internet. See the [Security Policy](SECURITY.md) for what to do if you are planning to run it.

There are 2 ways to run the project:
1. The simplest way to run the project is using Docker containers which will run the full-fledged VulnerableApplication with all the components. For running as Docker application, follow the following steps:
    1. Download and Install [Docker Compose](https://docs.docker.com/compose/install/)
    2. Clone this GitHub repository
    3. Open the terminal and navigate to the project root directory
    4. Run the command `docker-compose pull && docker-compose up`
    5. Navigate to your browser and visit `http://localhost` to access the VulnerableApp UI
    6. Mailpit is also available at `http://localhost/mailpit/` for viewing emails captured by the local SMTP server.

    **Note**: The above steps will run the latest unreleased VulnerableApp version. If you want to run the latest released version, please use the Docker `latest` tag.
2. Another way to run the VulnerableApp is as a standalone application:
    1. Navigate to the [Releases Section](https://github.com/SasanLabs/VulnerableApp/releases) on GitHub and download the JAR for the latest released version
    2. Open the terminal and navigate to the project root directory
    3. Run the command `java -jar VulnerableApp-*`
    4. Navigate to your browser and visit `http://localhost:9090/VulnerableApp` to access the legacy UI

## Building the project
There are 2 ways in which this project can be built and used:
1. As a Docker application which will help in running the full-fledged VulnerableApplication. For running as Docker application, follow these steps:
    1. Build the docker image by running `./gradlew jibDockerBuild`
    2. Download [Docker Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) and run it in the same directory using `docker-compose up`
    3. Navigate to your browser and visit `http://localhost` to access the VulnerableApp UI
2. As a Spring Boot application which will run with the legacy UI or REST API but gives the benefit of debugging and solving issues:
    1. Import the project into your favorite IDE and run it
    2. Navigate to your browser and visit `http://localhost:9090/VulnerableApp` to access the legacy VulnerableApp UI which you can use to debug and test

## Contributing to Project

There are multiple ways in which you can contribute to the project:
1. If you are a developer and trying to get started on the project, the suggestion is to go through the list of [issues](https://github.com/SasanLabs/VulnerableApp/issues) which contains `good first issues`.
2. If you are a developer or a security professional looking to add a new vulnerability type, you can generate the sample vulnerability by running `./gradlew GenerateSampleVulnerability`. It will create a new vulnerability stub for you to extend.
3. In case you are looking to contribute to the project by publicizing it or helping with growth, please feel free to add your thoughts to the discussions section or issues and we can work together on that.

## Testing with Modern UI
VulnerableApp-facade provides a modern UI for VulnerableApp. To test your local changes with the Modern UI:

1. **Prerequisite**: Ensure you have Docker and Docker Compose installed.
2. **Run Testing Script**:
   - On Windows: `./scripts/testWithModernUI.bat`
   - On Linux/Mac: `./scripts/testWithModernUI.sh`

This script will build your local changes into a Docker image (`sasanlabs/owasp-vulnerableapp:unreleased`) and start the full stack (including facade, jsp, and php services) using `docker-compose`.

3. **Access the UI**: Navigate to `http://localhost` to see the modern UI with your changes.
4. **Access Mailpit**: Navigate to `http://localhost/mailpit/` to view emails captured by the local SMTP server.

## Technologies used
- Java 17
- Spring Boot
- ReactJS
- JavaScript/TypeScript

### Connecting to embedded H2 database
For accessing the database from the browser, visit: `http://localhost:9090/VulnerableApp/h2`

Database Connection properties:
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
