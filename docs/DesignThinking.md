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


## Problems with the Current Design ##
1. Each Vulnerability requires a different UserInterface, Http Method etc. Now the problem is we are giving a way to extend and add vulnerabilities so there should be a way given to add the UserInterface, Http Method etc too. 

## Suggestions and Changes ##
Solutions :-
```
1. Adding a new method to CustomController Interface which returns the ResponseBean with UI changes. 

Issues with above appraoch :-
    1.1 Tight Coupling of Backend and Frontend
    1.2 URL rewriting, ie some intelligent way to distinguish a get call for the Template or execution 
    of vulnerability. One solution is URL without level is for Template and with Level is for 
    Vulnerability.
    
2. Giving a way to specify the JS file in the Annotation and entire template is same for all the 
vulnerability except the JS file. so Network call is only made to backend to execute the vulnerability 
and whenever UI/index.html is loaded it will create a state like Redux State in React which holds info
of JS file for each vulnerability and as User chooses a vulnerability type  in UI it will load the JS 
file and show it to user. Then after selecting level a Backend call is made.

Issues with above approach :-
    1.1 XSS related tests still holds the tight coupling of Backend and Frontend.
    1.2 Handling of Post is still questionable ie we need to figure out a way to handle posts but 
    certainly this approach makes it little easier.
```
