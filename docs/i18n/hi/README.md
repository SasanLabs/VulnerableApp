---
layout: default
title: Hindi
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![लाइसेंस](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Gradle के साथ Java CI](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs का स्वागत है](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## इसे तोड़ें। इसे स्कैन करें। इसे पुनरुत्पादित करें। इसके विरुद्ध बेंचमार्क करें। इसे बेहतर बनाएं।

OWASP VulnerableApp एक मॉड्यूलर, जानबूझकर असुरक्षित बनाया गया एप्लिकेशन है, जिसे मुख्य रूप से पुनरुत्पादित किए जा सकने वाले परीक्षण परिदृश्यों के माध्यम से सुरक्षा स्कैनरों के सत्यापन और बेंचमार्किंग के लिए डिज़ाइन किया गया है, साथ ही यह सीखने और प्रयोग करने में भी सहायता करता है।

### 🔍 इसे अलग क्या बनाता है
पारंपरिक असुरक्षित एप्लिकेशनों के विपरीत, VulnerableApp को एक परीक्षण योग्य सुरक्षा पारिस्थितिकी तंत्र के रूप में डिज़ाइन किया गया है, न कि एक स्थिर प्रशिक्षण ऐप के रूप में।

### यह सक्षम बनाता है:

- 🔬 Burp Suite, OWASP ZAP और कस्टम DAST इंजनों जैसे टूल्स के लिए स्कैनर बेंचमार्किंग
- 🧩 मॉड्यूलर भेद्यता डिज़ाइन, जो कोर सेवाओं में बदलाव किए बिना नए परिदृश्यों को जोड़ने की अनुमति देता है
- 📊 विभिन्न रिलीज़ और परिवेशों में सुरक्षा रिग्रेशन परीक्षण
- 🎯 आधुनिक वेब एप्लिकेशन पैटर्न के लिए यथार्थवादी आक्रमण सतह का अनुकरण
- 🧪 दोहराए जा सकने वाले स्कैनिंग परिणामों के लिए निर्धारक भेद्यता व्यवहार
- 🧠 सुरक्षा इंजीनियरों, शोधकर्ताओं और शिक्षकों के लिए निर्मित

![पूरा आर्किटेक्चर स्टैक](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)

### VulnerableApp आपकी सहायता करता है:

- ज्ञात भेद्यता पैटर्न के विरुद्ध सुरक्षा टूल्स के व्यवहार का सत्यापन करने में
- सुरक्षा प्रयोगों के लिए नियंत्रित परिवेश बनाने में
- नई आक्रमण तकनीकों के उभरने पर भेद्यता कवरेज का विस्तार करने में
- सुसंगत और दोहराए जा सकने वाले सुरक्षा परीक्षण पाइपलाइनों को चलाने में

### ⚙️ यह महत्वपूर्ण क्यों है

अधिकांश असुरक्षित ऐप्स:
- स्थिर होते हैं
- उनका विस्तार करना कठिन होता है
- केवल मैनुअल सीखने के लिए डिज़ाइन किए जाते हैं

### VulnerableApp बनाया गया है:
स्वचालन, पुनरुत्पादन क्षमता और विकास के लिए

### उपयोगकर्ता इंटरफ़ेस ###
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## प्रोजेक्ट चलाना
प्रोजेक्ट चलाने के 2 तरीके हैं:
1. प्रोजेक्ट चलाने का सबसे सरल तरीका Docker कंटेनरों का उपयोग करना है, जो सभी घटकों के साथ पूर्ण VulnerableApplication चलाएंगे। Docker एप्लिकेशन के रूप में चलाने के लिए निम्न चरणों का पालन करें:
    1. [Docker Compose](https://docs.docker.com/compose/install/) डाउनलोड करें और इंस्टॉल करें
    2. इस Github रिपॉज़िटरी को क्लोन करें
    3. टर्मिनल खोलें और प्रोजेक्ट की रूट डायरेक्टरी में जाएँ
    4. कमांड चलाएँ ```docker-compose pull && docker-compose up```
    5. ब्राउज़र खोलें और `http://localhost` पर जाएँ। इससे VulnerableApp का उपयोगकर्ता इंटरफ़ेस खुलेगा।
    6. स्थानीय SMTP सर्वर द्वारा कैप्चर किए गए ईमेल देखने के लिए Mailpit `http://localhost:8025` पर भी उपलब्ध है।

    **नोट**: उपरोक्त चरण नवीनतम अप्रकाशित VulnerableApp संस्करण चलाएंगे। यदि आप नवीनतम प्रकाशित संस्करण चलाना चाहते हैं, तो Docker का **latest** टैग उपयोग करें।
2. VulnerableApp को एक स्वतंत्र Vulnerable Application के रूप में चलाने का दूसरा तरीका:
    1. Github के [Releases Section](https://github.com/SasanLabs/VulnerableApp/releases) में जाएँ और नवीनतम प्रकाशित संस्करण की Jar डाउनलोड करें
    2. टर्मिनल खोलें और प्रोजेक्ट की रूट डायरेक्टरी में जाएँ
    3. कमांड चलाएँ ```java -jar VulnerableApp-*```
    4. ब्राउज़र खोलें और `http://localhost:9090/VulnerableApp` पर जाएँ। इससे VulnerableApp का Legacy उपयोगकर्ता इंटरफ़ेस खुलेगा।

## प्रोजेक्ट का निर्माण
इस प्रोजेक्ट को बनाने और उपयोग करने के 2 तरीके हैं:
1. Docker एप्लिकेशन के रूप में, जो पूर्ण VulnerableApplication चलाने में मदद करेगा। Docker एप्लिकेशन के रूप में चलाने के लिए:
    1. `./gradlew jibDockerBuild` चलाकर Docker इमेज बनाएँ
    2. [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) डाउनलोड करें और उसी डायरेक्टरी में `docker-compose up` चलाएँ
    3. ब्राउज़र में `http://localhost` पर जाएँ। इससे VulnerableApp का उपयोगकर्ता इंटरफ़ेस खुलेगा।
2. SpringBoot एप्लिकेशन के रूप में, जो Legacy UI या Rest API के साथ चलेगा, लेकिन डीबगिंग और समस्याओं के समाधान का लाभ देगा। यह सबसे सरल तरीका है:
    1. प्रोजेक्ट को अपनी पसंदीदा IDE में इम्पोर्ट करें और चलाएँ
    2. ब्राउज़र में `http://localhost:9090/VulnerableApp` पर जाएँ। इससे VulnerableApp का Legacy उपयोगकर्ता इंटरफ़ेस खुलेगा, जिसका उपयोग आप डीबग और परीक्षण के लिए कर सकते हैं।

## प्रोजेक्ट में योगदान

आप कई तरीकों से प्रोजेक्ट में योगदान कर सकते हैं:
1. यदि आप डेवलपर हैं और प्रोजेक्ट पर शुरुआत करना चाहते हैं, तो [issues](https://github.com/SasanLabs/VulnerableApp/issues) की सूची देखें, जिसमें `good first issue` शामिल हैं और जो शुरुआती योगदान के लिए उपयुक्त हैं।
2. यदि आप डेवलपर या सुरक्षा पेशेवर हैं और नई भेद्यता जोड़ना चाहते हैं, तो `./gradlew GenerateSampleVulnerability` चलाकर Sample Vulnerability उत्पन्न करें। इससे प्लेसहोल्डर और टिप्पणियों सहित एक टेम्पलेट बनेगा। संशोधित फ़ाइलें कमांड के लॉग या Github इतिहास में देखी जा सकती हैं। उन फ़ाइलों में जाकर प्लेसहोल्डर भरें और परिवर्तन देखने के लिए प्रोजेक्ट बनाएं।
3. यदि आप प्रोजेक्ट का प्रचार करके या उसके विकास में योगदान देना चाहते हैं, तो Discussions सेक्शन या Issues में अपने विचार साझा करें। हम उन पर चर्चा करेंगे।

## आधुनिक UI के साथ परीक्षण
VulnerableApp-facade, VulnerableApp के लिए एक आधुनिक UI प्रदान करता है। अपने स्थानीय परिवर्तनों का आधुनिक UI के साथ परीक्षण करने के लिए:

1. **पूर्वापेक्षा**: सुनिश्चित करें कि Docker और Docker-Compose इंस्टॉल हैं।
2. **परीक्षण स्क्रिप्ट चलाएँ**:
   - Windows पर: `.\scripts\testWithModernUI.bat`
   - Linux/Mac पर: `./scripts/testWithModernUI.sh`

यह स्क्रिप्ट आपके स्थानीय परिवर्तनों को Docker इमेज (`sasanlabs/owasp-vulnerableapp:unreleased`) में बनाएगी और `docker-compose.local.yml` का उपयोग करके पूर्ण स्टैक (facade, jsp और php सेवाओं सहित) प्रारंभ करेगी।

3. **UI तक पहुँचें**: अपने परिवर्तनों के साथ आधुनिक UI देखने के लिए `http://localhost` पर जाएँ।

4. **Mailpit तक पहुँचें**: स्थानीय SMTP सर्वर द्वारा कैप्चर किए गए ईमेल देखने के लिए `http://localhost:8025` पर जाएँ।

## उपयोग की गई तकनीकें
- Java17
- Spring Boot
- ReactJS
- Javascript/TypeScript

### एम्बेडेड H2 डेटाबेस से कनेक्ट करना
ब्राउज़र से डेटाबेस तक पहुँचने के लिए जाएँ: `http://localhost:9090/VulnerableApp/h2`

डेटाबेस कनेक्शन गुण:
```properties
JDBC Url: jdbc:h2:mem:testdb
User Name: admin
Password: hacker
```

## वर्तमान में समर्थित भेद्यता प्रकार

1. [JWT भेद्यता](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/jwt/)
2. [Command Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/commandInjection)
3. [Cryptography Failures](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/cryptographicFailures)
4. [File Upload Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/fileupload)
5. [Path Traversal Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/pathTraversal)
6. [SQL Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection)
    1. [Error Based SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java)
    2. [Union Based SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java)
    3. [Blind SQLi](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java)
7. [XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss)
    1. [Persistent XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/persistent)
    2. [Reflected XSS](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xss/reflected)
8. [XXE](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/xxe)
9. [Open Redirect](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/openRedirect)
    1. [Http 3xx Status code based](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/service/vulnerability/urlRedirection/Http3xxStatusCodeBasedInjection.java)
10. [SSRF](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ssrf)
11. [IDOR](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/idor)
12. [Clickjacking](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/clickjacking)
13. [LDAP Injection](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/ldapInjection)
14. [Authentication Vulnerability](https://github.com/SasanLabs/VulnerableApp/tree/master/src/main/java/org/sasanlabs/service/vulnerability/authentication)

## अपने स्कैनर का बेंचमार्क करें

VulnerableApp एक comparator के साथ आता है जो स्कैनर के निष्कर्षों का प्रोजेक्ट के अंतर्निहित ग्राउंड ट्रुथ के विरुद्ध मूल्यांकन करता है और coverage / missed / unmatched रिपोर्ट तैयार करता है। DAST और SAST दोनों स्कैनर एक ही एंडपॉइंट के माध्यम से समर्थित हैं:

- एंडपॉइंट: `POST http://<baseurl>/VulnerableApp/scanner/benchmark`
- अनुरोध बॉडी — अपने स्कैनर के अनुसार उपयुक्त संरचना चुनें:
  - DAST: `{ tool, scanType: "DAST", findings: [ { url, type, cwe, wascId } ] }` (`scanType` वैकल्पिक है और डिफ़ॉल्ट रूप से `DAST` होता है; `type`/`cwe`/`wascId` अलग-अलग वैकल्पिक हैं — किसी एक अक्ष पर मेल पर्याप्त है)
  - SAST: `{ tool, scanType: "SAST", findings: [ { filePath, line, cwe, type } ] }`
- प्रतिक्रिया बॉडी और डिस्क पर `benchmarks/<tool>-results.json`: कवरेज रिपोर्ट

स्कैनर को स्वयं चलाना इस दायरे से बाहर है — आपको JSON प्रदान करना होगा। पूर्ण इनपुट/आउटपुट स्कीमा, मिलान नियम, मानक भेद्यता-प्रकार शब्दावली और `curl` उदाहरणों के लिए [`benchmarks/README.md`](benchmarks/README.md) देखें।

## संपर्क
यदि आप किसी चरण में अटक जाते हैं या प्रोजेक्ट तथा उसके उद्देश्यों से संबंधित कुछ समझना चाहते हैं, तो karan.sasan@owasp.org पर ईमेल भेजें या [issue](https://github.com/SasanLabs/VulnerableApp/issues) दर्ज करें। हम आपकी सहायता करने का पूरा प्रयास करेंगे।

## दस्तावेज़ीकरण और संदर्भ

1. [दस्तावेज़ीकरण](https://sasanlabs.github.io/VulnerableApp)
2. [डिज़ाइन दस्तावेज़ीकरण](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
3. [Owasp VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
4. [OWASP Spotlight श्रृंखला का अवलोकन वीडियो](https://www.youtube.com/watch?v=HRRTrnRgMjs)
5. [अवलोकन वीडियो](https://www.youtube.com/watch?v=AjL4B-WwrrA&ab_channel=OwaspVulnerableApp)

### ब्लॉग

1. [Owasp-VulnerableApp का अवलोकन - Medium लेख](https://hussaina-begum.medium.com/an-extensible-vulnerable-application-for-testing-the-vulnerability-scanning-tools-cc98f0d94dbc)
2. [Owasp-VulnerableApp का अवलोकन - Blogspot पोस्ट](https://hussaina-begum.blogspot.com/2020/10/an-extensible-vulnerable-application.html)
3. [Kenji Nakajima द्वारा Owasp VulnerableApp का परिचय](https://jpn.nec.com/cybersecurity/blog/220520/index.html)
4. [Gen AI आधारित प्लेटफ़ॉर्म Shannon द्वारा VulnerableApp का उपयोग](https://qiita.com/fiord/items/9351bcff6d646862f181)
5. [मैंने OWASP ZAP File Upload Addon बनाया। यहाँ बताया गया है कि VulnerableApp-Facade पहले क्यों आवश्यक था](https://medium.com/p/52c4f2226ed3)

### OWASP VulnerableApp का उपयोग

1. [वैश्विक शैक्षणिक रुचि देखें](./docs/Usage.md)

### समस्या निवारण संदर्भ

1. [SQL Injection Vulnerability का उपयोग करते हुए Reddit](https://www.reddit.com/r/hacking/comments/11wtf17/owasp_vulnerableappfacade_sql_injection/)

### अन्य भाषाओं में Readme

1. [रूसी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ru/README.md)
2. [चीनी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/zh-CN/README.md)
3. [हिन्दी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/hi/README.md)
4. [पंजाबी](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/pa/README.md)
5. [कोरियाई](https://github.com/SasanLabs/VulnerableApp/tree/master/docs/i18n/ko/README.md)
