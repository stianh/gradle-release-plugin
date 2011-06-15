package no.entitas.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

class GitVersion {
    private final Project project
    String versionNumber
    Boolean release = null
    def releaseTagPattern = ~/^\S+-REL-\d+$/

    def GitVersion(project) {
        this.project = project

        project.gradle.taskGraph.whenReady {graph ->
            if (graph.hasTask(':releasePrepare')) {
				releasePreConditions()
                release = true
                this.versionNumber = getNextTagName()
            }
            else if (isOnReleaseTag() && !hasLocalModifications()) {
                release = true
                this.versionNumber = getCurrentVersion()
            }
            else {
                release = false
                this.versionNumber = getCurrentBranchName() + '-SNAPSHOT'
            }
        }
    }
    //TODO:Ensure that we are on a branch
	def releasePreConditions(){
		if (hasLocalModifications()) {
            throw new RuntimeException('Uncommited changes found in the source tree:\n' + getLocalModifications())
        }
        if (isOnReleaseTag()) {
            throw new RuntimeException('No changes since last tag.')
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


    def releasePrepare() {
        def newTag = versionNumber
        tag(newTag, "Release Tag: ${newTag}")
    }

    def releasePerform() {
        pushTags()
    }


	def getLocalModifications() {
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

    def isOnReleaseTag() {
        def stdout = new ByteArrayOutputStream()
        try {
            def x = project.exec {
                executable = 'git'
                args = ['describe', '--exact-match', 'HEAD']
                standardOutput = stdout
            }
            def tagName = stdout.toString().replaceAll("\\n","")

            return releaseTagPattern.matcher(tagName).matches()
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
            new VersionNumber(stdout.toString().replaceAll("\\n", ""))
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
        def latestTagVersion = getLatestTag(currentBranch)
        if (latestTagVersion == null) {
			return "$currentBranch-REL-1"
        }
		latestTagVersion.nextVersionTag()
    }

    def getLatestTag(String currentBranch) {
        def tagSearchPattern = "${currentBranch}-REL-*"
        def tags = getTagNames(tagSearchPattern)
        
		if (tags != null) {
			def versionNumbers = tags.collect{new VersionNumber(it)}
            groovy.util.GroovyCollections.max(versionNumbers)
        }
    }

    def getTagNames(String tagSearchPattern) {
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