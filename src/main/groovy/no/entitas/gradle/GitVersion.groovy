package no.entitas.gradle

import java.text.SimpleDateFormat
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

class GitVersion {
    private final Project project
    String versionNumber
    Boolean release = null

    def GitVersion(project) {
        this.project = project

        project.gradle.taskGraph.whenReady {graph ->
            if (graph.hasTask(':releasePrepare')) {
                release = true
                this.versionNumber = getNextTagName()
            }
            else if (isOnTag() && !hasLocalModifications()) {
                release = true
                this.versionNumber = getCurrentVersion()
            }
            else {
                release = false
                this.versionNumber = getCurrentBranchName() + '-SNAPSHOT'
            }
        }

    }

    String toString() {
        versionNumber
    }


    boolean isRelease() {
        if (release == null) {
            throw new GradleException("Can't determine whether this is a release build before the task graph is populated")
        }
        return release
    }

    String getDistributionUrl() {
        if (release) {
            'http://releases'
        } else {
            'http://snapshots'
        }
    }

    def releasePrepare() {
        if (hasLocalModifications()) {
            throw new RuntimeException('Uncommited changes found in the source tree:\n' + getLocalModifications())
        }
        if (isOnTag()) {
            throw new RuntimeException('No changes since last tag.')
        }
        def newTag = versionNumber
        tag(newTag, "Release Tag: ${newTag}")
    }

    def releasePerform() {
        if (hasLocalModifications()) {
            throw new RuntimeException('Uncommited changes found in the source tree:\n' + getLocalModifications())
        }
        if (!isOnTag()) {
            throw new RuntimeException('Can not do releasePerform from other than a tag commit.')
        }
        pushTags()
    }


	def getLocalModifications() {
		  println 'checking for modifications'
	        def stdout = new ByteArrayOutputStream()
	        project.exec {
	            executable = 'git'
	            args = ['status', '--porcelain']
	            standardOutput = stdout
	        }
	        if (stdout.toByteArray().length > 0) {
	            return stdout.toString()
	        }
	}
    def hasLocalModifications() {
      getLocalModifications() == null ? false : true
    }

    def isOnTag() {
        def stdout = new ByteArrayOutputStream()
        try {
            def x = project.exec {
                executable = 'git'
                args = ['describe', '--exact-match', 'HEAD']
                standardOutput = stdout
            }
            return true
        } catch (ExecException e) {
            return false
        }
    }

    def getCurrentVersion() {
        def stdout = new ByteArrayOutputStream()
        try {
            def x = project.exec {
                executable = 'git'
                args = ['describe', '--exact-match', 'HEAD']
                standardOutput = stdout
            }
            stdout.toString().replaceAll("\\n", "")
        } catch (ExecException e) {
            throw new RuntimeException("Not on a tag.")
        }
    }

    def getCurrentBranchName() {
        def stdout = new ByteArrayOutputStream()
        project.exec {
            executable = 'git'
            args = ['name-rev', '--name-only', 'HEAD']
            standardOutput = stdout
        }
        stdout.toString().replaceAll("\\n", "")
    }

    def getNextTagName() {
        def currentBranch = getCurrentBranchName()
        def latestTagName = getLatestTag(currentBranch)
        if (latestTagName == null) {
			return "$currentBranch-REL-1"
        }
        def tagNameParts = latestTagName.split('-')
        def newVersion = tagNameParts[-1].toInteger() + 1
        latestTagName.replaceAll(tagNameParts[-1], newVersion.toString())
    }

    def getLatestTag(String currentBranch) {
        println "Getting latest tag for branch ${currentBranch}"
        def tagSearchPattern = "${currentBranch}-REL-*"
        def tags = getTags(tagSearchPattern)
        if (tags != null) {
            tags[-1]
        }
    }

    def getTags(String tagSearchPattern) {
        def stdout = new ByteArrayOutputStream()
        project.exec {
            executable = 'git'
            args = ['tag', '-l', tagSearchPattern]
            standardOutput = stdout
        }
        if (stdout.toByteArray().length > 0) {
            def allReleases = stdout.toString()
            allReleases.split('\n')
        }
    }

    def tag(String tag, String message) {
        println "tagging with $tag"
        project.exec {
            executable = 'git'
            args = ['tag', '-a', tag, '-m', message]
        }
    }

    def pushTags() {
        println "pushing tags"
        project.exec {
            executable = 'git'
            args = ['push', '--tags']
        }
    }
}