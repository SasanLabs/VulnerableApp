# How to start this tool #
For Starting VulnerableApp you have following ways:
1. For running it as a docker container, Please visit [Docker Repository](https://hub.docker.com/r/sasanlabs/owasp-vulnerableapp)
2. This tool is a spring boot based app so for starting this app you need to import this project in IDE like eclipse or intellij. IDE should have buildship/gradle plugin as this is developed using gradle. however this app supports maven too but it is not tested fully using maven. As this is developed in eclipse so eclipse is recommended IDE for now.
After importing the project, run the app and it should start a server and try to go to `http://<base-url>:9090/allEndPoint` , for eg: `http://localhost:9090/allEndPoint` url and it should return a json like structure (More information in below section).
3. Downloading released jar from [Releases](https://github.com/SasanLabs/VulnerableApp/releases) and run the application by executing following command ```java -jar  VulnerableApp-1.0.0.jar ```


# How we can use this project for learning security #
There can be difficulties in using this tool reason being the User interface is not yet developed fully.
so following are the simple steps on how to use this tool:
1. For finding information about the type of vulnerabilities present in the application is:
   - go to `http://<base-url>:9090/allEndPoint` , for eg: `http://localhost:9090/allEndPoint`
   - choose the type of vulnerability you want to learn like say `XSS`
   - now choose the vulnerability name and level for eg: vulnerablity name: `UrlParamWithNullByteBasedImgTagAttrInjection` and 
  vulnerability level `LEVEL_1`
   - now you can build the url as `http://<base-url>:9090/vulnerable/<vulnerability name>/<vulnerability level>` for eg:
  `http://localhost:9090/vulnerable/UrlParamWithNullByteBasedImgTagAttrInjection/LEVEL_1`
2. Now from step 1.4 we know how to find the vulnerable endpoint 
3. Now using active scanner like burp or zap or by running manual scanning you can try to exploit that vulnerability

### Current UserInterface
Currently UserInterface is build for only 2 vulnerability types JWT vulnerabilities and Cross Site Scripting in Image tag. To look at them Please visit `http://<base-url>:9090`
