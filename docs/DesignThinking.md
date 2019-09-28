# Design thinking/brainstorming for VulnerableApp #

## Problems we are trying to solve ##
```
1. Easy way to test ZAP/Burp scanner tools
2. Learning Security vulnerablities, Exploits and Remediations.
```
## Desired output ##
A Simple Web Application which has an :-
```
1. easy way to Add new Vulnerabilities (As every day new technology is coming and it has a different set of vulnerabilities)
2. easy way to Add Vulnerable code to existing vulnerabilities (New Attack vectors introduction)
3. easy way to Expose Vulnerable code.
```
## Analsis on approaches and issues ##
Looking at the requirements/desired output we can think of something that can be easily pluggable and extensible.


| Major Points | Outcome |
|--------------|---------|
| From 1 and 2 | Configuration driven, better annotation based SpringBoot Approach|
| From 1 and 2 it should be pluggable and Extensible| Might be Interfaces.|
| From point 3 Pluggable code| Might be some basic template based design |
| From Point 3 | We want to easily expose vulnerable code so Vulnerablity adding should not require Controller modifications|

So from above we can say approach should be Interfaces and Annotation based.

## Current Design ##
Component Diagram :-

Gliffy :- [ComponentDiagram Gliffy](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/VulnerableApp%20ComponentDesign.gliffy)

Image :- [ComponentDiagram png](https://github.com/SasanLabs/VulnerableApp/blob/master/docs/ComponentDiagram%20VulnerableApp.png)

More Granular Diagram :-



## Problems with the Current Design ##

## Suggestions and Changes ##

