---
layout: default
title: How To Use
nav_order: 3
---

# How to run the project
 
## Running as full-fledged Vulnerable Application 
1. It is very easy to run the VulnerableApp as full-fledged VulnerableApplication. Follow the steps mentioned at [Simple Start](https://github.com/SasanLabs/VulnerableApp-facade#simple-start) and it is quite straightforward.

## Running as standalone Vulnerable Application
1. Running it as a **docker container** in order to run it as docker container, you just need to run `docker run --rm -p 9090:9090 --name=owasp-vulnerableapp sasanlabs/owasp-vulnerableapp:latest`

2. Running it as an **executable**, download released jar from [Releases](https://github.com/SasanLabs/VulnerableApp/releases) and run the application by executing following command `java -jar  VulnerableApp-1.0.0.jar`

3. Running it by **Building manually**, as VulnerableApp is a spring boot based application so for starting this application you need to import this project in IDE like eclipse or intellij. IDE should have buildship/gradle plugin. As this project is developed in eclipse so eclipse is recommended IDE for now.
After importing the project, run the app and it should start a server.

After starting the application, Navigate to `http://<base-url>:9090/VulnerableApp` , for eg: `http://localhost:9090/VulnerableApp` url and a User Interface will guide you to next steps. Running as standalone Vulnerable Application gives the **Legacy User Interface**

# How to build the project
There are 2 ways in which this project can be built and used:
1. As a SringBoot application which will run with the Legacy UI or Rest API but gives the benefit of debugging and solving issues. This is the simple way, 
    1. Import the project into your favorite IDE and run it
    2. Navigate to browser and visit: `http://localhost:9090/VulnerableApp` and this will give the Legacy User Interface for VulnerableApp which you can use to debug and test.
2. As a Docker application which will help in running the full-fledged VulnerableApplication. For running as Docker application, follow following steps:
    1. Build the docker image by running `./gradlew jibDockerBuild`
    2. Download [Docker-Compose](https://github.com/SasanLabs/VulnerableApp-facade/blob/main/docker-compose.yml) and run in the same directory `docker-compose up`
    3. Navigate to browser and visit `http://localhost` and this will give the User Interface for VulnerableApp.

## Glimpse of ReactJS based User Interface ##
![VulnerableApp-facade UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp-facade/main/docs/images/gif/VulnerableApp-Facade.gif)

## Glimpse of the Legacy User Interface ##
![VulnerableApp-Legacy UI](https://raw.githubusercontent.com/SasanLabs/VulnerableApp/master/docs/gifs/VulnerableApp.gif)

# How SAST or DAST can use the project
As VulnerableApp is built for scanning tools hence there are multiple ways in which they can leverage the VulnerableApp.
1. Developer of these scanning tools can add new vulnerabilities for testing there scan rules, payload testing etc. They can even go with TDD approach where they first write the Vulnerable code and then they can write the Scan Rules.
2. For Evaluation of scanning tools
   1. For DAST, VulnerableApp exposes an endpoint `http://<baseurl>/VulnerableApp/scanner` which provides information about all the Vulnerabilities present in the VulnerableApp which DAST tools can leverage to evaluate themselves.
   2. For SAST, we have added a [ExpectedIssues.csv](https://github.com/SasanLabs/VulnerableApp/blob/master/scanner/sast/expectedIssues.csv) file which has vulnerabilities and their line numbers which SAST tools can use to evaluate themselves.

## Details about scanner endpoint
scanner endpoint exposes following information which DAST tools can leverage:
```
url: The URL of the endpoint in VulnerableApp. 
variant: Whether the endpoint is SECURE or UNSECURE. SECURE is helpful in figuring out the false positives.
method: Type of HTTP method for the endpoint like GET or POST
vulnerabilityTypes: List of vulnerability types present in the endpoint to validate if scanner is fully finding all the vulnerabilities in an endpoint.
```
Note: [VulnerabilityTypes](https://github.com/SasanLabs/VulnerableApp/blob/master/src/main/java/org/sasanlabs/vulnerability/types/VulnerabilityType.java) are custom values as no single standard represent all the Vulnerabilities. However we are working on creating a mapping between VulnerabilityType and CWE/WASC.
## Details about ExpectedIssues.csv
[ExpectedIssues.csv](https://github.com/SasanLabs/VulnerableApp/blob/master/scanner/sast/expectedIssues.csv) contains following information which SAST tools can leverage:
```
Vulnerability Type: type of vulnerability present
CWE: CWE id for the Vulnerability
WASC: WASC id for the Vulnerability
File: Full path of the file containing the Vulnerability
Line: Line number in the file containing the Vulnerability
Source: Number of times that line is executed. 
```
