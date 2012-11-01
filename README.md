# Gradle release plugin (Git and Subversion) [![Build Status](https://secure.travis-ci.org/stianh/gradle-release-plugin.png?branch=develop)](http://travis-ci.org/stianh/gradle-release-plugin)

This is a Gradle plugin that makes it very simple to automate release management when using
git or Subversion as vcs. The plugin is responsible for knowing the version to build at all
times. You should not use this plugin if you want/need to be in control of the version
name/number.

In the case of a normal gradle build, the plugin generates a version name based on the
current branch name ${branchName}-SNAPSHOT.

If you run the task releasePrepare, the plugin will query git/svn for the latest release tag
and add one to the number. The artifacts will have a version name like master-REL-1 (if this
is the first time you run :releasePrepare). A release tag with the version name will be
created in git/svn. (The tag will NOT be pushed in this task when using git.)

If you want the artifacts to be uploaded and the releaseTag to be pushed, run the
releasePerform task. This task depends on releasePrepare so it will run first.

**Notice:** The build will fail if you try to run releasePrepare again, with an error message
telling you that there is no changes since the last release tag. If you want to rebuild a
release, just run a normal gradle build and the plugin will figure out that the current HEAD
is a release tag and use the release version. The same applies if you checkout a release tag
and run build.

## Usage

Add the following to your build file to setup where the plugin should be downloaded:

```groovy
apply plugin: 'gitrelease' // or apply plugin: 'svnrelease'

buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath group: 'no.entitas.gradle', name: 'gradle-release-plugin', version: '1.14'
  }
}
```

**Notice:** This *must* be in the root build file in a multi-module build, that is,
the release plugin can only be applied at the top level.

To setup where your artifacts should be deployed, use a regular `uploadArchives` section.
This is an example of deploying to a Maven repository:

```groovy
uploadArchives {
  doFirst {
    repositories.mavenDeployer {
    uniqueVersion = false

    repository(url: '...release distribution url...') {
      //resolved from gradle.properties
      authentication(userName: project.username, password: project.password)
    }

    snapshotRepository(url: '...snapshot distribution url...') {
      //resolved from gradle.properties
      authentication(userName: project.username, password: project.password)
    }
  }
}
```

In a multi-module build this will typically be setup for each subproject that needs to be
deployed.

## Configuration

The closure below shows the available configuration options and their default values:

```groovy
release {
    failOnSnapshotDependencies = false
    versionStrategy = { currentVersion ->
        if (System.properties['release.version']) {
            System.properties['release.version']
        } else {
            new BigDecimal(currentVersion).add(BigDecimal.ONE).toPlainString()
        }
    }
    startVersion = { currentBranch -> "1" }
}
```

**`failOnSnapshotDependencies`** when set to true the build will fail if it has any snapshot dependencies.

**`versionStrategy`** a closure for calculating the next version number, given the current (as a String).
The default implementation is to add 1, or if the system property *release.version* is set, use its value.

**`startVersion`** which version to start counting from.


## Tasks

**releasePrepare**  
* Checks that there are no local modifications (git/svn status)
* Checks that your current HEAD is not a release tag
* The projects are built with version resolved from the latest git release tag + 1
* Creates a git tag for your current head named ${branchName}-REL-${version}  

**releasePerform**  
* This task depends on the :releasePrepare task
* Depends on uploadArtifacts and pushes tags if using git


## Known issues and limitations

* Only tested on Java projects
* The releasePerform task has only been tested with Nexus and http upload