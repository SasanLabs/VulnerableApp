# VulnerableApp ![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) ![Java CI with Maven](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Maven/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)


There are many tools for finding Web Application Vulnerability like [ZAP](https://github.com/zaproxy), Burp etc but while contributing to these Open Source Tools 
it is very tough to check if the Payloads added or Scripts added are working as expected.

## Solutions
``` 
1. Generally developers are writing vulnerable applications for testing Payloads before 
contributing to these tools or
2. Writing Unit test cases
```
Both the above approaches are having flaws like 
```
Approach 1: One developer's vulnerable application can be reused by other developers and 
as everyone is writing there own applications it is like doing rework again and also there might be chances of various bugs in those applications
which are not reviewed by anyone

Approach 2: Simulating every scenario using unit tests are not feasible.
```

Solutions provided by [VulnerableApp](https://github.com/SasanLabs/VulnerableApp):
```
1. Simple SpringBoot Application which is very easy to run.
2. Simple Annotation based and URL Path based vulnerable code execution
3. Easy integration with any of the Web Application Vulnerability Finding Tools.
4. Scenarios included are very common and exploitable using any Vulnerability finding tools.
5. Tried to cover as many scenarios while contributing code to ZAP
```

Other Benefits :-
```
1. Learning Web Application Security is very tough, as it is not legal and 
2. there are very less vulnerable application for Security testing.
```

This Application is deliberately made vulnerable to attacks so that users can learn not only about attacks and attack vectors but also about securing Web Applications against those attacks.

## How to use this tool ##
[How to use guide](https://github.com/SasanLabs/VulnerableApp/blob/master/HOW-TO-USE.md)

## How can Vulnerability Scanning tools use VulnerableApp ? ##
VulnerableApp is specifically designed for the Vulnerability Scanning Tools like ZAP wherein few endpoints are exposed only for helping them.
Following are the endpoints exposed:
[Scanner Endpoint](/scanner)
[SiteMap Endpoint](/sitemap.xml)

### Scanner Endpoint ###
Scanner is specially crafted endpoint to provide information about each vulnerability present in VulnerableApp.
#### Sample Json Response ####
```
[
  {
    "url": "http://192.168.0.148:9090/vulnerable/JWTVulnerability/LEVEL_1",
    "location": "QUERY_PARAM",
    "parameter": "JWT",
    "sampleValues": [
      "ey.."
    ],
    "method": "GET",
    "vulnerabilityTypes": [
      "CLIENT_SIDE_VULNERABLE_JWT"
    ]
  }
]
```
Following is the Json Response explanation:
- url of the vulnerable endpoint
- location of the parameter like Query Param/Header etc.
- method like GET/POST accepted by vulnerable endpoint
- parameter name which represents the input to the endpoint
- type of vulnerabilities exposed by the endpoint
- Sample input to the endpoint

As Vulnerability Scanning Tools use `sitemap.xml`, `robots.txt` etc in order to find the exposed endpoints so we have provided sitemap which provides all the vulnerable endpoints present in the VulnerableApp. In case Vulnerability Scanning Tools wants to use VulnerableApp, you need to understand the output of Scanner endpoint and only that information alone can suffice for all needs. These tools can also use sitemap for the same but that might not suffice for all usecases.


## Contributing to Project ##
Contributing to opensource is always good from learning prespective as open source is the community for learn-help-grow-ing together. 
We really appreciate the contribution to this project but as this project is in its initial phase so we have not set any guidelines so if you are interested in contributing to this project please send an email to preetkaran20@gmail.com and we will try our best to onboard you to this project. if you are already onboarded please raise a Github Pull Request, we will review and merge that into the master repository.

### Another way of Contribution ###
As we have added some of the vulnerabilities so in case you are trying to learn some vulnerability and you have found some different kind of vulnerability which is not known to us or it is unintentionally not being addressed, please let us know by raising an **issue**  with Title containing **$Vulnerability_Missed$** so that we can add those vulnerable payload or vulnerabilities in code.

## Contact ##
Please raise a github issue for enhancement/issues in VulnerableApp or send email to preetkaran20@gmail.com regarding queries
we will try to resolve issues asap.

## Website ##
[VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
