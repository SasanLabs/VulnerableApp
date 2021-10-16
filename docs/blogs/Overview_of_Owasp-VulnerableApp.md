---
layout: default
title: Overview of Owasp VulnerableApp
has_children: false
parent: Blogs
---

## An extensible Vulnerable Application for testing the Vulnerability scanning tools

Anyone working in the cyber security domain or starting with cyber security will be introduced to one or the other Vulnerable applications as their playground. These applications are used to understand each vulnerability and to learn about how to mitigate them. I was introduced to BWAPP(Buggy Web Application) and DVWA(Damn Vulnerable Web Application).

The great thing about these applications(BWAPP and DVWA) is that they cover a lot of vulnerabilities from the OWASP list of vulnerabilities. However, whenever the Vulnerability Scanner Tool developer has a new requirement to add a new scan rule or to modify an existing scan rule, these existing vulnerability scanning applications will not be able to help.

The existing vulnerable applications are not meeting the demands of developers of vulnerability scanning tools, the developers end up writing their own vulnerable applications to test their scanning tools and those applications are not reviewed or maintained. They are usually forgotten once the testing needs are met for the vulnerability scanning tool.

```
These vulnerable applications should be reviewed and maintained, so that
multiple vulnerability scanning tools could make effective use of these
vulnerable applications
```

{: .fs-5 }

A tweet by OWASP ZAP project lead highlighting the lack of well maintained vulnerable applications targeting vulnerability scanning tools -

<blockquote class="twitter-tweet"><p lang="en" dir="ltr">Not that many. The ones I know of are:<br>- <a href="https://t.co/cduavcFRYO">https://t.co/cduavcFRYO</a><br>- <a href="https://t.co/d5DVDYl2ag">https://t.co/d5DVDYl2ag</a><br>- <a href="https://t.co/QMIL906Qxa">https://t.co/QMIL906Qxa</a> *<br>- <a href="https://t.co/uhtl2ilPRb">https://t.co/uhtl2ilPRb</a> *<br>- <a href="https://t.co/NdkheyXTYh">https://t.co/NdkheyXTYh</a><br>- <a href="https://t.co/M9i8xcrEcW">https://t.co/M9i8xcrEcW</a><br><br>Only the 2 stared are being maintained right now.</p>&mdash; Simon Bennetts (@psiinon) <a href="https://twitter.com/psiinon/status/1293844526390480896?ref_src=twsrc%5Etfw">August 13, 2020</a></blockquote> <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>

I came across a new vulnerable web Application, named [VulnerableApp](https://sasanlabs.github.io/VulnerableApp/), which has taken care of the above concerns and it can be easily extended to add yet another vulnerable scenario into the web application. The major advantage of this VulnerableApp is that developers need not spend extra effort in writing a new Vulnerable Application and have the new scenarios covered. VulnerableApp, since its open source application and its constantly being reviewed and updated, it can be used by various scanning tools.

VulnerableApp can be extended to include new code to test any new scan rule in the development. Scanner Tool development teams can leverage this property to have a TDD(test driven development) model, where the application should have the vulnerable code first, before a scan rule is written to identify the vulnerability.

All the new code that's added to the VulnerableApp will serve as the database of possible vulnerabilities and these can be used for learning/training or benchmarking a scanning tool or it can even be used for organizing CTFs. Currently, this application is developed using Java8, Spring Boots and Vanilla Javascript. It can be extended to use other technologies in future.

It also has various ways to deploy it, as a docker container or as an executable(jar file) or we can build the application and deploy it locally.

To extend the VulnerableApp to include a new vulnerability or to modify the existing vulnerability, the below documentation link can be referred: https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html
VulnerableApp is also listed as an incubating project under owasp.org as a tool for scanning vulnerabilities: https://owasp.org/www-project-vulnerableapp/

## Website

[VulnerableApp](https://owasp.org/www-project-vulnerableapp/)
