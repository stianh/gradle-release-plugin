Gradle release plugin(Git and Subversion)
[![Build Status](https://secure.travis-ci.org/stianh/gradle-release-plugin.png)](http://travis-ci.org/stianh/gradle-release-plugin)
================================
This is a very simple gradle plugin for automating release management when using git or Subversion as vcs.  
The plugin is responsible for knowing the version to build at all times.  
You should not use this plugin if you want/need to be in control of the version name/number.  

In the case of a normal gradle build, the plugin generates a version name based on the current branch name ${branchName}-SNAPSHOT.

If you run the task releasePrepare, the plugin will query git/svn for the latest release tag and add one.  
The artifacts will have a version name like master-REL-1 (if this is the first time you run :releasePrepare)  
A release tag with the version name will be created in git/svn. (The tag will NOT be pushed in this task when using git.)

If you want the artifacts to be uploaded to Nexus and the releaseTag to be pushed, run the releasePerform task.(Will run the releasePrepare also.)

**Notice:** The build will fail if you try to run releasePrepare again, with an error message telling you that there is no changes since the last release tag.  
If you want to rebuild a release, just run a normal gradle build and the plugin will figure out that the current HEAD is a release tag and use the release version.
The same applies if you checkout a release tag and run build.

Installation 
------------
Clone the repo
run: gradle clean install 

Usage:
------

Use the plugin:

```groovy
apply plugin: 'gitrelease' // or apply plugin: 'svnrelease'

buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath group: 'no.entitas.gradle', name: 'gradle-release-plugin', version: '1.11'
  }
}

// In a subproject that you want to be deployed to a Maven repository
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

**Notice:** In a multi-project build, the release plugin should only be applied at the top level. 

Tasks
------
**releasePrepare**  
* Checks that there are no local modifications (git/svn status)
* Checks that your current HEAD is not a release tag
* The projects are built with version resolved from the latest git release tag + 1
* Creates a git tag for you current head named ${branchName}-REL-${version}  

**releasePerform**  
* This task depends on the :releasePrepare task
* Depends on uploadArtifacts and pushes tags if using git

Known issues and limitations
-------------
* Only java project support at the moment
* Output from git invocations leaks to the console
* The releasePerform task has only been tested with Nexus and http upload