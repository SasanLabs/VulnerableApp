# VulnerableApp

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
