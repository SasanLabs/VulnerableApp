# Releasing VulnerableApp #

VulnerableApp is Leveraging GitHub workflow and actions for creating new releases automatically.
When committing the feature, we have the option to increment the version's to major, minor, or patch value
by including <code>+semver:[major|minor|patch]</code> in the commit message. Major, minor, patch values are the
strings 'major', 'minor', and 'patch'.

Examples:
```properties
git commit -m "some text +semver:major"
git commit -m "+semver:minor some text"
git commit -m "+semver:patch some text"
```
By default, if the version is not provided in the commit message, then patch is incremented.
Examples of version change considering the current version is 1.10.0:
1. For patch release, the newer version will be 1.10.1
2. For minor release, the newer version will be 1.11.0
3. for major release, the newer version will be 2.0.0

The github action will create a tag for the latest published release on GitHub and DockerHub.

More information can be found at [Semantic 
Versioning 
Specifiction](https://semver.org/)
