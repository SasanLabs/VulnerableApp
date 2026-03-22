---
layout: default
title: Hindi
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

आज के दौर में वेब एप्लिकेशन तेज़ी से लोकप्रिय हो रहे हैं, जिसके साथ-साथ उन्हें सुरक्षित करने की आवश्यकता भी उतनी ही बढ़ रही है। हालाँकि कई वल्नरेबिलिटी स्कैनिंग टूल उपलब्ध हैं, लेकिन इन्हें विकसित करते समय डेवलपर्स को इनका परीक्षण करना पड़ता है और यह भी जानना पड़ता है कि ये टूल कितने प्रभावी हैं। इस समय ऐसे बहुत कम जानबूझकर कमज़ोर एप्लिकेशन उपलब्ध हैं जिन पर इन टूल का परीक्षण किया जा सके। बाज़ार में कुछ ऐसे एप्लिकेशन ज़रूर हैं, लेकिन वे इस उद्देश्य को ध्यान में रखकर नहीं बनाए गए थे और इसलिए उनमें विस्तारित करने की सुविधा का अभाव है।

**VulnerableApp** इन सभी समस्याओं को ध्यान में रखकर बनाया गया है। यह प्रोजेक्ट स्केलेबल, एक्स्टेंसिबल, इंटीग्रेट करने में आसान और सीखने में सरल है।

### यूज़र इंटरफ़ेस

![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## उपयोग की गई प्रौद्योगिकियाँ

- Java 17
- Spring Boot
- ReactJS
- Javascript/TypeScript

## वर्तमान में समर्थित वल्नरेबिलिटी प्रकार

1. [JWT वल्नरेबिलिटी](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [कमांड इंजेक्शन](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [क्रिप्टोग्राफ़ी विफलताएँ](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [फ़ाइल अपलोड वल्नरेबिलिटी](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [पाथ ट्रैवर्सल वल्नरेबिलिटी](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL इंजेक्शन](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [एरर-बेस्ड SQL इंजेक्शन](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [यूनियन-बेस्ड SQL इंजेक्शन](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [ब्लाइंड SQL इंजेक्शन](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [परसिस्टेंट XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [रिफ्लेक्टेड XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [ओपन रिडायरेक्ट](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection)
    1. [HTTP 3xx स्टेटस कोड आधारित](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)

## प्रोजेक्ट में योगदान

आप निम्नलिखित तरीकों से प्रोजेक्ट में योगदान कर सकते हैं:
1. यदि आप एक डेवलपर हैं और प्रोजेक्ट से अभी शुरुआत कर रहे हैं, तो [इश्यूज़](https://github.com/SasanLabs/VulnerableApp/issues) की सूची देखें जिनमें `good first issue` टैग है।
2. यदि आप नया वल्नरेबिलिटी प्रकार जोड़ना चाहते हैं, तो `./gradlew GenerateSampleVulnerability` कमांड चलाएँ। यह एक सैंपल टेम्पलेट बनाएगा — उन फ़ाइलों को खोलें, प्लेसहोल्डर भरें और प्रोजेक्ट बिल्ड करें।
3. यदि आप प्रोजेक्ट का प्रचार-प्रसार करके योगदान देना चाहते हैं, तो डिस्कशन सेक्शन या इश्यूज़ में अपने विचार साझा करें।

## प्रोजेक्ट चलाना

प्रोजेक्ट चलाने के दो तरीके हैं:
1. Docker कंटेनर का उपयोग करना (सभी घटकों सहित पूरा VulnerableApp):
    1. [Docker Compose](https://docs.docker.com/compose/install/) डाउनलोड और इंस्टॉल करें
    2. इस GitHub रिपॉज़िटरी को क्लोन करें
    3. टर्मिनल खोलें और प्रोजेक्ट के रूट डायरेक्टरी पर जाएँ
    4. कमांड ```docker-compose pull && docker-compose up``` चलाएँ
    5. ब्राउज़र में `http://localhost` पर जाएँ।

    **नोट**: नवीनतम रिलीज़्ड संस्करण के लिए Docker **latest** टैग का उपयोग करें।

2. स्टैंडअलोन एप्लिकेशन के रूप में:
    1. GitHub के [रिलीज़ सेक्शन](https://github.com/SasanLabs/VulnerableApp/releases) से नवीनतम JAR फ़ाइल डाउनलोड करें
    2. कमांड ```java -jar VulnerableApp-*``` चलाएँ
    3. ब्राउज़र में `http://localhost:9090/VulnerableApp` पर जाएँ।

## प्रोजेक्ट बिल्ड करना

1. Docker एप्लिकेशन के रूप में:
    1. `./gradlew jibDockerBuild` से Docker इमेज बनाएँ
    2. [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) डाउनलोड करें और `docker-compose up` चलाएँ
    3. ब्राउज़र में `http://localhost` पर जाएँ।
2. SpringBoot एप्लिकेशन के रूप में (डीबगिंग के लिए):
    1. प्रोजेक्ट को अपनी IDE में इम्पोर्ट करें और चलाएँ
    2. ब्राउज़र में `http://localhost:9090/VulnerableApp` पर जाएँ।

### एम्बेडेड H2 डेटाबेस से कनेक्ट करना

ब्राउज़र से डेटाबेस एक्सेस करने के लिए: `http://localhost:9090/VulnerableApp/h2`

```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## संपर्क करें

किसी भी समस्या के लिए karan.sasan@owasp.org पर ईमेल करें या एक [इश्यू](https://github.com/SasanLabs/VulnerableApp/issues) उठाएँ।

## दस्तावेज़ और संदर्भ

1. [दस्तावेज़ीकरण](https://sasanlabs.github.io/VulnerableApp)
2. [डिज़ाइन दस्तावेज़ीकरण](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [OWASP VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [OWASP Spotlight श्रृंखला का अवलोकन वीडियो](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [अवलोकन वीडियो](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### ब्लॉग

1. [OWASP-VulnerableApp का अवलोकन — Medium लेख](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [OWASP-VulnerableApp का अवलोकन — Blogspot पोस्ट](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Kenji Nakajima द्वारा OWASP VulnerableApp का परिचय](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [जेनरेटिव AI आधारित प्लेटफ़ॉर्म Shannon](https://qiita.com/fiord/items/9351bcff6d646862f181)

### समस्या निवारण संदर्भ

1. [Reddit: SQL इंजेक्शन वल्नरेबिलिटी का उपयोग](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### अन्य भाषाओं में README

1. [रूसी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [चीनी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [पंजाबी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
