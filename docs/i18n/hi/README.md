---
layout: default
title: Hindi
parent: Locale
---
# ![OWASP VulnerableApp](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/logos/Coloured/iconColoured.png) OWASP VulnerableApp

![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) ![](https://img.shields.io/github/v/release/SasanLabs/VulnerableApp?style=flat) [![लाइसेंस](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Gradle के साथ Java CI](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) [![PRs का स्वागत है](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com) [![Docker Pulls](https://badgen.net/docker/pulls/sasanlabs/owasp-vulnerableapp?icon=docker&label=pulls)](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp/) [![codecov](https://codecov.io/gh/SasanLabs/VulnerableApp/graph/badge.svg?token=DTS3PA8WXZ)](https://codecov.io/gh/SasanLabs/VulnerableApp)

## इसे तोड़ें। इसे स्कैन करें। इसे पुनरुत्पादित करें। इसके विरुद्ध बेंचमार्क करें। इसे बेहतर बनाएँ।

OWASP VulnerableApp एक मॉड्यूलर, जानबूझकर असुरक्षित बनाया गया एप्लिकेशन है, जिसे मुख्य रूप से पुनरुत्पादित किए जा सकने वाले परीक्षण परिदृश्यों के माध्यम से सुरक्षा स्कैनरों का सत्यापन और बेंचमार्क करने के लिए डिज़ाइन किया गया है, साथ ही यह सीखने और प्रयोग करने का भी समर्थन करता है।

### 🔍 इसे अलग क्या बनाता है
पारंपरिक असुरक्षित एप्लिकेशनों के विपरीत, VulnerableApp को एक परीक्षण योग्य सुरक्षा पारिस्थितिकी तंत्र (security ecosystem) के रूप में डिज़ाइन किया गया है, न कि एक स्थिर प्रशिक्षण एप्लिकेशन के रूप में।

### यह सक्षम बनाता है:

- 🔬 Burp Suite, OWASP ZAP और कस्टम DAST इंजन जैसे टूल्स के लिए स्कैनर बेंचमार्किंग
- 🧩 मॉड्यूलर भेद्यता डिज़ाइन, जो कोर सेवाओं में बदलाव किए बिना नए परिदृश्य जोड़ने की अनुमति देता है
- 📊 विभिन्न रिलीज़ और वातावरणों में सुरक्षा प्रतिगमन (regression) परीक्षण
- 🎯 आधुनिक वेब एप्लिकेशन पैटर्न के लिए यथार्थवादी अटैक सरफेस सिमुलेशन
- 🧪 दोहराए जा सकने वाले स्कैनिंग परिणामों के लिए निर्धारक (deterministic) भेद्यता व्यवहार
- 🧠 सुरक्षा इंजीनियरों, शोधकर्ताओं और शिक्षकों के लिए निर्मित

![संपूर्ण आर्किटेक्चर स्टैक](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/logos/sasanlabs.png)

### VulnerableApp आपकी सहायता करता है:

- ज्ञात भेद्यता पैटर्न पर सुरक्षा टूल्स के व्यवहार का सत्यापन करने में
- सुरक्षा प्रयोगों के लिए नियंत्रित वातावरण बनाने में
- नई आक्रमण तकनीकों के उभरने पर भेद्यता कवरेज का विस्तार करने में
- सुसंगत और दोहराए जा सकने वाले सुरक्षा परीक्षण पाइपलाइन चलाने में

### ⚙️ यह महत्वपूर्ण क्यों है

अधिकांश असुरक्षित एप्लिकेशन होते हैं:
- स्थिर
- विस्तार करना कठिन
- केवल मैनुअल सीखने के लिए डिज़ाइन किए गए

### VulnerableApp बनाया गया है:
स्वचालन (automation), पुनरुत्पादन (reproducibility) और विकास (evolution) के लिए

### उपयोगकर्ता इंटरफ़ेस ###
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## प्रोजेक्ट चलाना
प्रोजेक्ट चलाने के 2 तरीके हैं:
1. प्रोजेक्ट चलाने का सबसे सरल तरीका Docker कंटेनरों का उपयोग करना है, जो सभी घटकों सहित पूर्ण VulnerableApplication चलाएगा। Docker एप्लिकेशन के रूप में चलाने के लिए निम्नलिखित चरणों का पालन करें:
    1. [Docker Compose](https://docs.docker.com/compose/install/) डाउनलोड और इंस्टॉल करें
    2. इस Github रिपॉज़िटरी को क्लोन करें
    3. टर्मिनल खोलें और प्रोजेक्ट की रूट डायरेक्टरी पर जाएँ
    4. कमांड चलाएँ ```docker-compose pull && docker-compose up```
    5. ब्राउज़र में `http://localhost` खोलें। इससे VulnerableApp का उपयोगकर्ता इंटरफ़ेस मिलेगा।
    6. स्थानीय SMTP सर्वर द्वारा कैप्चर किए गए ईमेल देखने के लिए Mailpit `http://localhost:8025` पर भी उपलब्ध है।

    **नोट**: ऊपर दिए गए चरण नवीनतम अप्रकाशित VulnerableApp संस्करण चलाएँगे। यदि आप नवीनतम प्रकाशित संस्करण चलाना चाहते हैं, तो Docker का **latest** टैग उपयोग करें।
2. VulnerableApp को एक स्टैंडअलोन Vulnerable Application के रूप में चलाने का दूसरा तरीका:
    1. Github के [Releases Section](https://github.com/SasanLabs/VulnerableApp/releases) पर जाएँ और नवीनतम प्रकाशित संस्करण की Jar डाउनलोड करें
    2. टर्मिनल खोलें और प्रोजेक्ट की रूट डायरेक्टरी पर जाएँ
    3. कमांड चलाएँ ```java -jar VulnerableApp-*```
    4. ब्राउज़र में `http://localhost:9090/VulnerableApp` खोलें। इससे VulnerableApp का Legacy User Interface मिलेगा।

## प्रोजेक्ट बनाना (Building the project)
इस प्रोजेक्ट को बनाने और उपयोग करने के 2 तरीके हैं:
1. Docker एप्लिकेशन के रूप में, जो पूर्ण VulnerableApplication चलाने में सहायता करेगा। Docker एप्लिकेशन के रूप में चलाने के लिए:
    1. `./gradlew jibDockerBuild` चलाकर Docker इमेज बनाएँ
    2. [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) डाउनलोड करें और उसी डायरेक्टरी में `docker-compose up` चलाएँ
    3. ब्राउज़र में `http://localhost` खोलें। इससे VulnerableApp का उपयोगकर्ता इंटरफ़ेस मिलेगा।
2. SpringBoot एप्लिकेशन के रूप में, जो Legacy UI या Rest API के साथ चलेगा और डिबगिंग तथा समस्याओं के समाधान का लाभ देगा। यह सबसे सरल तरीका है:
    1. प्रोजेक्ट को अपने पसंदीदा IDE में इम्पोर्ट करें और चल
