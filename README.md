# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![](https://img.shields.io/twitter/follow/sasan_karan?style=flat&logo=twitter)](https://twitter.com/intent/follow?screen_name=sasan_karan)


As Web Applications are becoming popular these days, there comes a dire need to secure them. Although there are several Vulnerability Scanning Tools, however while developing these tools, developers need to test them. Moreover, they also need to know how well is the Vulnerability Scanning tool performing. As of now, there are little or no such vulnerable applications existing for testing such tools. There are Deliberately Vulnerable Applications existing in the market but they are not written with such an intent and hence lag extensibility, e.g. adding new vulnerabilities is quite difficult. Hence, the developers resort to writing their own vulnerable applications, which usually causes productivity loss and the pain to rework.

**VulnerableApp** is built keeping these factors in mind. This project is scalable, extensible, easier to integrate and easier to learn.
As solving the above issue requires addition of various vulnerabilities, hence it becomes a very good platform to learn various security vulnerabilities.

### Newer React based User Interface ###
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

### Older Interface ###
![Owasp Vulnerable Graphic Representation](/docs/gifs/VulnerableApp.gif)

### Future Goal

Going further, this application might becomes a database for vulnerabilities. Hence, in future, it can be used for hosting CTFs and can also become a compliance/benchmark for Vulnerability Scanning tools.

## Project Setup

[Setup Guide](https://sasanlabs.github.io/VulnerableApp/HOW-TO-USE.html)

## Technologies used
- Java8
- Spring Boot
- Vanilla Javascript

#### Note: we are not limited to these technologies and if required, open to expand to other technologies.
    
## Currently handled Vulnerability types

1. [JWT Vulnerability](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [Command Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [File Upload Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
4. [Path Traversal Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
5. [SQL Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [Error Based SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [Union Based SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [Blind SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
6. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [Persistent XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [Reflected XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
7. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
8. [Open Redirect](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection)
    1. [Http 3xx Status code based](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)

## Contributing to Project

Contributing to open source is always good from learning perspective as open source is the community to collaborate and grow together.

We really appreciate contributions to this project. As this project is in it's initial phase, we have not set any guidelines. So, feel free to shoot a mail at karan.sasan@owasp.org or raise an [issue](https://github.com/SasanLabs/VulnerableApp/issues) and we will try our best to onboard you to this project. If you are already onboarded, we actively welcome your Pull Requests. Visit [Design Documentation](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html) for internal implementation details.

You can also raise an issue, in case you are looking for learning some kind of vulnerability which is not present in VulnerableApp. We will try to add that vulnerability ASAP!

## Documentation in other languages

1. [Russian](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [Chinese](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [Hindi](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
4. [Punjabi](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)

## Contact

Please raise an [issue](https://github.com/SasanLabs/VulnerableApp/issues) or send an email to karan.sasan@owasp.org for any queries.
We will try to resolve the issues ASAP.

## Other details

1. [Documentation](https://sasanlabs.github.io/VulnerableApp)
2. [Owasp VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
3. [Overview Video](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

## Blogs
1. [Overview of Owasp-VulnerableApp - Medium article](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [Overview of Owasp-VulnerableApp - Blogspot post](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
