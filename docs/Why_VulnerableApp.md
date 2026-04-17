---
layout: default
title: Why VulnerableApp
nav_order: 2
---
### Why VulnerableApp

There are many tools for finding Web Application Vulnerability like [ZAP](https://github.com/zaproxy), Burp, etc but while contributing to these Tools 
it is very tough to check if the Scan Rule added is working properly. The Vulnerable Applications present in the market are not written with Scanners as their targets. 

A tweet by OWASP ZAP project lead highlighting the lack of well maintained vulnerable applications targeting vulnerability scanning tools -

<blockquote class="twitter-tweet"><p lang="en" dir="ltr">Not that many. The ones I know of are:<br>- <a href="https://t.co/cduavcFRYO">https://t.co/cduavcFRYO</a><br>- <a href="https://t.co/d5DVDYl2ag">https://t.co/d5DVDYl2ag</a><br>- <a href="https://t.co/QMIL906Qxa">https://t.co/QMIL906Qxa</a> *<br>- <a href="https://t.co/uhtl2ilPRb">https://t.co/uhtl2ilPRb</a> *<br>- <a href="https://t.co/NdkheyXTYh">https://t.co/NdkheyXTYh</a><br>- <a href="https://t.co/M9i8xcrEcW">https://t.co/M9i8xcrEcW</a><br><br>Only the 2 stared are being maintained right now.</p>&mdash; Simon Bennetts (@psiinon) <a href="https://twitter.com/psiinon/status/1293844526390480896?ref_src=twsrc%5Etfw">August 13, 2020</a></blockquote> <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>

### What used to be the solution?

1. Tool developers had to manually write applications that are prone to separate security flaws 
that the tool they are developing is able to flag.
2. Slog it out with unit tests

### Why would the above not work?

Approach 1: Tool developers have to focus on ways to crash test their apps, which is cumbersome 
as each might have to develop their own versions of a flawed web-app, which are not reviewed 
and debugging those apps might quickly become overwhelming.

Approach 2: Simulating every scenario using unit tests are not feasible.

### What we propose
#### The [VulnerableApp](https://github.com/SasanLabs/VulnerableApp):

1. A simple web application built on top of the popular Spring framework
2. Docker ready, can simply fire up the image and we are good to go
3. Fully extensible, annotation and path based vulnerability execution
4. Simple integration with security tools since VulnerableApp exposes REST APIs
5. Developed from experience gathered collaborating for ZAP

Other Benefits :-

1. Going further it will become a Bank of Vulnerabilities and hence can be used as a platform for learning Web Application Security
2. It can also become a very good benchmarking platform to compare between multiple releases and across various scanning tools.
3. Scanners can follow the practise of writing a Vulnerability first before exposing it using scanner something like a TDD approach.

This Application is deliberately vulnerable to attacks so that users can learn not only about attacks and attack vectors but also about securing Web Applications against those attacks.
