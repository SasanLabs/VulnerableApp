# How to start this tool #
This tool is a spring boot based app so for starting this app you need to import this project in IDE like eclipse or intellij. IDE should have buildship/gradle plugin as this is developed using gradle. however this app supports maven too but it is not tested fully using maven. As this is developed in eclipse so eclipse is recommended IDE for now.
After importing the project, run the app and it should start a server and try to go to `http://<base-url>:9090/allEndPoint` , for eg: `http://localhost:9090/allEndPoint` url and it should return a json like structure (More information in below section).

we are working on making this app available as a tomcat embedded jar, for now only way available is to setup using IDE.

# How we can use this project for learning security #
There can be difficulties in using this tool reason being the User interface is not yet developed.
so following are the simple steps on how to use this tool:
1. For finding information about the type of vulnerabilities present in the application is:
  1.1 go to `http://<base-url>:9090/allEndPoint` , for eg: `http://localhost:9090/allEndPoint`
  1.2 choose the type of vulnerability you want to learn like say `XSS`
  1.3 now choose the vulnerability name and level for eg: vulnerablity name: `UrlParamWithNullByteBasedImgTagAttrInjection` and 
  vulnerability level `LEVEL_1`
  1.4 now you can build the url as `http://<base-url>:9090/vulnerable/<vulnerability name>/<vulnerability level>` for eg:
  `http://localhost:9090/vulnerable/UrlParamWithNullByteBasedImgTagAttrInjection/LEVEL_1`
2. Now from step 1.4 we know how to find the vulnerable endpoint 
3. Now using active scanner like burp or zap or by running manual scanning you can try to exploit that vulnerability

### Note ###
we are developing a simple UI where we will give a table structure with list of vulnerability types and links to them so that it is easier for user to access the vulnerable links with specific vulnerability. 
For accessing this simple UI please go to `http://<base-url>:9090`

Target state for the UI is something like a Master Detail view where master will show the name of vulnerability and vulnerability types and detail view contains all the details of vulnerability like way to attack/hints, how we can protect our websites against this kind of vulnerability/attacks.
