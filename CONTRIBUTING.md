# Contributing to OWASP VulnerableApp
VulnerableApp is a community project, and we are always delighted to welcome new contributors!

There are lots of ways you can contribute:

## Got a Question or Problem?
If you have a question or problem related to using VulnerableApp then the first thing to do is to raise an issue on the VulnerableApp repo: https://github.com/SasanLabs/VulnerableApp/issues
or send an email to karan.sasan@owasp.org 

## Found an Issue?
If you have found a bug then raise an issue on the VulnerableApp repo: https://github.com/SasanLabs/VulnerableApp/issues

## Have a Feature Request?
If you have a suggestion for new functionality then you can raise an issue on the VulnerableApp repo: https://github.com/SasanLabs/VulnerableApp/issues

Its worth checking to see if its already been requested, and including as much information as you can so that we can fully understand your requirements.

### Adding code to VulnerableApp
Fixing [issues](https://github.com/SasanLabs/VulnerableApp/issues) is very valuable (ones flagged as [Good First issue](https://github.com/SasanLabs/VulnerableApp/issues?q=is%3Aopen+is%3Aissue++label%3A%22good+first+issue%22+) are good ones to start on) and there are always many improvements we want to make.

For more understanding on VulnerableApp's internal framework please visit: [Design Documentation](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
#### Guidelines for Pull Request (PR) submission and processing:

##### Format/Style Java Code

The Java code is formatted according to Google Java Style (AOSP variant). The build automatically checks
that the code conforms to the style (using [Spotless], which delegates to [google-java-format]), it can
also be used to format the code (with the Gradle task `spotlessApply`) if the IDE/editor in use
does not support it.

##### What should you, the author of a pull request, expect from us (VulnerableApp Team)?
* How much time (maximum) until the first feedback? 1 week.
* And following iterations? 1 week.
* This is a guideline we should normally be able to hit. If it's been more than a week and you haven't heard then please feel free to add a comment to your PR.
