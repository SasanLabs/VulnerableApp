# How to start this tool #
Following are the ways in which you can use the VulnerableApp:
1. Running it as a **docker container**, Please visit [Docker Repository](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp)
2. Running it as an **executable**, Downloading released jar from [Releases](https://github.com/SasanLabs/VulnerableApp/releases) and run the application by executing following command `java -jar  VulnerableApp-1.0.0.jar`
3. Running it by **Building manually**, as VulnerableApp is a spring boot based application so for starting this application you need to import this project in IDE like eclipse or intellij. IDE should have buildship/gradle plugin or maven. As this project is developed in eclipse so eclipse is recommended IDE for now.
After importing the project, run the app and it should start a server and try to go to `http://<base-url>:9090` , for eg: `http://localhost:9090` url and a User Interface will guide you to next steps.


# How can Vulnerability Scanning tools use VulnerableApp ? #
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

## Glimpse of the tool ##
Tool's Starting Screen:
![Welcome](/docs/Starting%20Screen.png)

On Clicking **Learn Security** following screen pops up:
![Learning Security](docs/Vulnerability%20list.png)
This screen contains the List of Vulnerabilities, there information and useful links for learning more about them.

On Clicking **Practice Vulnerability** following screen pops up:
![Practice Vulnerability](docs/Choosing%20Vulnerability%20Level%20Screen.png)
This screen contains list of Levels present under the selected vulnerability, help at each level etc.

#### Get Set Go for the Journey of Learning about the Web Application Security, MR. Security Enthusiast !!! ####
