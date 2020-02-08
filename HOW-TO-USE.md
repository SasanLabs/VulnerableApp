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
