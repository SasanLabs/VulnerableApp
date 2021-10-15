---
layout: default
title: How To Use
nav_order: 3
---

# How to run the project
 
## React based User Interface 
1. Running it as a full-fledged VulnerableApplication for learning and testing is very easy. Follow the steps mentioned at [Simple Start](https://github.com/SasanLabs/VulnerableApp-facade#simple-start) and it is quite straightforward.

## Legacy way with Legacy User Interface
1. Running it as a **docker container** in order to run it as docker container, you just need to run `docker run --rm -p 9090:9090 --name=owasp-vulnerableapp sasanlabs/owasp-vulnerableapp:latest`

2. Running it as an **executable**, download released jar from [Releases](https://github.com/SasanLabs/VulnerableApp/releases) and run the application by executing following command `java -jar  VulnerableApp-1.0.0.jar`

3. Running it by **Building manually**, as VulnerableApp is a spring boot based application so for starting this application you need to import this project in IDE like eclipse or intellij. IDE should have buildship/gradle plugin. As this project is developed in eclipse so eclipse is recommended IDE for now.
After importing the project, run the app and it should start a server.

After starting the application, Navigate to `http://<base-url>:9090/VulnerableApp` , for eg: `http://localhost:9090/VulnerableApp` url and a User Interface will guide you to next steps.

# How to build the project
There are 2 ways in which this project can be built and used:
1. As a SringBoot application which will run with the Legacy UI or Rest API but gives the benefit of debugging and solving issues. This is the simple way, 
    1. Import the project into your favorite IDE and run it
    2. Navigate to browser and visit: `http://localhost:9090/VulnerableApp` and this will give the Legacy User Interface for VulnerableApp which you can use to debug and test.
2. As a Docker application which will help in running the full-fledged VulnerableApplication. For running as Docker application, follow following steps:
    1. Build the docker image by running `./gradlew jibDockerBuild`
    2. Download [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) and run in the same directory `docker-compose up`
    3. Navigate to browser and visit `http://localhost` and this will give the User Interface for VulnerableApp.

## Glimpse of React based User Interface ##
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## Glimpse of the Legacy User Interface ##
![VulnerableApp-Legacy UI](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/gifs/VulnerableApp.gif)

# How can Vulnerability Scanning tools use VulnerableApp ? #
VulnerableApp is specifically designed for the Vulnerability Scanning Tools like ZAP wherein we expose vulnerability definitions via the following endpoints:
- `VulnerableApp/scanner`
- `VulnerableApp/sitemap.xml`

### Scanner Endpoint ###
Scanner is a specifically crafted endpoint to provide information about each vulnerability present in VulnerableApp.
#### Sample Json Response ####
```
[
  {
    "url": "http://192.168.0.148:9090/vulnerable/JWTVulnerability/LEVEL_1",
    "location": "QUERY_PARAM",
    "parameter": "JWT",
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

As Vulnerability Scanning Tools use `sitemap.xml`, `robots.txt` etc. in order to find the exposed endpoints so we have provided sitemap which provides all the vulnerable endpoints present in the VulnerableApp. For a better usage of VulnerableApp, Vulnerability Scanning tools need to understand the output of `VulnerableApp/scanner` endpoint and that information alone can suffice for all needs. 
