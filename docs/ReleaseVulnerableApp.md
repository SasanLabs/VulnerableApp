# Releasing VulnerableApp #

VulnerableApp is leveraging multiple GitHub workflows and actions for creating new releases automatically.
The first workflow automates semantic versioning, repository tag creation, building and deploying as unreleased to 
DockerHub.
When committing a feature, we have the option to increment the version's major, minor, or patch value
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

More information can be found at [Semantic
   Versioning
   Specifiction](https://semver.org/)

The second workflow automates release creation. Using <code>workflow_dispatch</code>, this will get the latest 
repository tag, create a GitHub release, build and deploy as latest to DockerHub. When initializing this workflow, 
there is an option to provide release notes. 

To initialize the workflow, from the repository:
1. Select the 'Actions' tab.
2. Under 'Workflows' select 'Release project' workflow.
3. Select 'Run Workflow' where it says 'This workflow has a workflow_dispatch event trigger'.
4. Then select 'run workflow' again (can include optional release notes).

