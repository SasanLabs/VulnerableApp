---
layout: default
title: Why VulnerableApp
nav_order: 2
---
### Why VulnerableApp

There are many tools for finding Web Application Vulnerability like [ZAP](https://github.com/zaproxy), Burp etc but while contributing to these Open Source Tools 
it is very tough to check if the Payloads added or Scripts added are working as expected.

### What used to be the solution
``` 
1. Tool developers had to manually write applications that are prone to separate security flaws that the tool
they are developing is able to flag.
2. Slog it out with unit tests
```
### Why would the above not work 
```
Approach 1: Tool developers have to focus on ways to crash test their apps , which is cumbersome as each might have to
develop their own versions of a flawed web-app , debugging those apps might quickly become overwhelming 

Approach 2: Simulating every scenario using unit tests are not feasible.
```
### What we propose
#### The [VulnerableApp](https://github.com/SasanLabs/VulnerableApp):
```
1. A simple web app built on top of the popular Spring framework
2. Docker ready , can simply fire up the image and we are good to go
3. Fully extensible , annotation and path based vulnerability execution
4. Simple integration with security tools since VulnerableApp exposes REST APIs
5. Developed from experience gathered collaborating for ZAP
```

Other Benefits :-
```
1. Learning Web Application Security is very tough, as it is not legal and 
2. there are very less vulnerable application for Security testing.
```

This Application is deliberately vulnerable to attacks so that users can learn not only about attacks and attack vectors but also about securing Web Applications against those attacks.
