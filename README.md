Gradle Git release plugin
=========================
This is a very simple gradle plugin for automating release management when using git as vcs.
The plugin is responsible for knowing the version to build at all times.

In the case of a normal gradle build, the plugin generates a version name based on the current branch name ${branchName}-SNAPSHOT.

If you run the task releasePrepare, the plugin will query git for the latest release tag and add one.
The artifacts will have a version name like master-REL-1 (if this is the first time you run :releasePrepare)
A tag with the resolved version will be created in git.(The tag will NOT be pushed.)

If you want the artifacts to be uploaded to Nexus and the releaseTag to be pushed, run the releasePerform task.(Will run the releasePrepare also.)

**Notice:** The build will fail if you try to re run releasePrepare, with an error message telling you that there is no changes since the last release.
If you want to rebuild a release, just run a normal gradle build and the plugin will figure out that the current HEAD is a relase tag and use the release version.


Installation 
------------
Clone the repo
run: gradle clean install 

Usage:
------

Use the plugin:

<pre><code>
apply plugin: 'gitrelease'

buildscript {
  repositories {
    mavenRepo urls: ['file://' + new File(System.getProperty('user.home'), '.m2/repository').absolutePath]
  }
  dependencies {
    classpath group: 'no.entitas', name: 'gradle-git-release-plugin', version: '1.0-SNAPSHOT'
  }
}
//Configures the plugin 
gitRelease {
    snapshotDistributionUrl = 'http://{repo}/snapshots'
    releaseDistributionUrl = 'http://{repo}/releases'
}

//In a subproject that you want to be deployd in Nexus:
uploadArchives {
    doFirst {
        repositories.mavenDeployer {
            uniqueVersion = false
            if (version.release) {
                repository(url: releaseDistributionUrl) {
					//resolved from gradle.properties
                    authentication(userName: project.nexusUsername, password: project.nexusPassword)
                }
            } else {
				//resolved from gradle.properties
                repository(url: snapshotDistributionUrl) {
                    authentication(userName: project.nexusUsername, password: project.nexusPassword)
                }

            }
        }
    }
}

	
</code></pre>
	
**Notice:** In a multi-project build, the gitrelease plugin should only be applied at the top level. 

Tasks:
**releasePrepare**
* Checks that there are no local modifications. (Git status)
* Checks that your current HEAD is not a release tag.
* The projects are buildt with version resolved from the latest git release tag + 1.
* Creates a git tag for you current head named ${branchName}-REL-${version}
**releasPerform**
* This task depends on the :releasePrepare task, 
* Depends on uploadArtifacts and push the tag created in releasePrepare.

Add the shell variable ENSIME_HOME in TextMate -> Preferences... -> Advanced -> Shell Variables to the root of your ENSIME distribution (This would be the path to the folder you just unpacked). For me this is <code>/Users/Mads/dev/tools/emacs\_config/ensime\_2.8.1-0.3.8</code>

Known issues and limitations:
-------------
Not sure if it works with other than java projects atm.
Output from git invocations leaks to the console (might be confusing to the user that a fatal message from git is nothing to worry about.)
The releasePerform task has only been tested with Nexus and http upload.