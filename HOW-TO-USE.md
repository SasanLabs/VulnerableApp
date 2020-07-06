# How to start this tool #
For Starting VulnerableApp you have following ways:
1. For running it as a docker container, Please visit [Docker Repository](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp)
2. This tool is a spring boot based app so for starting this app you need to import this project in IDE like eclipse or intellij. IDE should have buildship/gradle plugin or maven. As this project developed in eclipse so eclipse is recommended IDE for now.
After importing the project, run the app and it should start a server and try to go to `http://<base-url>:9090` , for eg: `http://localhost:9090` url and a new User Interface will guide you for next steps.
3. Downloading released jar from [Releases](https://github.com/SasanLabs/VulnerableApp/releases) and run the application by executing following command ```java -jar  VulnerableApp-1.0.0.jar ```


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
