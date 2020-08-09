# VulnerableApp 
![OWASP Incubator](https://img.shields.io/badge/owasp-incubator-blue.svg) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Java CI with Gradle](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Gradle/badge.svg) ![Java CI with Maven](https://github.com/SasanLabs/VulnerableApp/workflows/Java%20CI%20with%20Maven/badge.svg) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

As Web Applications are becoming very popular these days, there comes the needs to secure them and there are many Vulnerability Scanning Tools but while developing those tools developers need to test those tools and not only that they need to know how well is the Vulnerability Scanning tool performing but there are no or very less such vulnerable applications exists for testing those tools. There are deliberately vulnerable applications exist in the market but they are not written with such an intent and hence lags extensibility e.g. adding new vulnerablities is quite difficult.

So generally developer write there own vulnerable applications but that cause productivity loss and also many times rework is done. This Project VulnerableApp is build keeping these factors in mind so this project is scalable, extensible, easiers to integrate and easier to learn.

As solving the above issue requires addition of various vulnerabilities, hence it becomes a very good platform to **learn various security vulnerabilities**. 

### Future Goal
As going further this application might becomes a database for vulnerabilities hence in future it can be used for **hosting CTF's** and also can be come a **compliance for Vulnerability Scanning tools.**

## How to use this tool ##
[How to use guide](https://github.com/SasanLabs/VulnerableApp/blob/master/HOW-TO-USE.md)

## How can Vulnerability Scanning tools use VulnerableApp ? ##
VulnerableApp is specifically designed for the Vulnerability Scanning Tools like ZAP wherein few endpoints are exposed only for helping them.
Following are the endpoints exposed:
- `/scanner`
- `/sitemap.xml`

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
- Sample input to the endpoint which helps in knowing the format of input like JWT's have a specific format.

As Vulnerability Scanning Tools use `sitemap.xml`, `robots.txt` etc in order to find the exposed endpoints so we have provided sitemap which provides all the vulnerable endpoints present in the VulnerableApp. For a better usage of VulnerableApp, Vulnerability Scanning tools need to understand the output of `\scanner` endpoint and that information alone can suffice for all needs. 

## Contributing to Project ##
Contributing to opensource is always good from learning prespective as open source is the community for learn-help-grow-ing together. 
We really appreciate the contribution to this project but as this project is in its initial phase so we have not set any guidelines so if you are interested in contributing to this project please send an email to preetkaran20@gmail.com or Raise an issue in the Repository and we will try our best to onboard you to this project. if you are already onboarded please raise a Github Pull Request, we will review and merge that into the master repository.

you can also raise an issue in case you are looking for learning some kind of vulnerability which is not present in the VulnerableApp, we will try to add that vulnerability asap.

## Contact ##
Please raise a github issue for enhancement/issues in VulnerableApp or send email to preetkaran20@gmail.com regarding queries
we will try to resolve issues asap.

## Website ##
[VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
