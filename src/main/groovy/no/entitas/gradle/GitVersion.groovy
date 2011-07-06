package no.entitas.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

class GitVersion implements Version {
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


        if(branchIsAheadOfRemote()){
            throw new RuntimeException('Project contains unpushed commits');
        }

	}

    boolean branchIsAheadOfRemote() {
       return gitExec(['status']).contains('Your branch is ahead of')
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
	    gitExec(['status', '--porcelain'])
	}
    def hasLocalModifications() {
      getLocalModifications() == null ? false : true
    }

    def isOnReleaseTag() {
        try {
            def gitResult = gitExec(['describe', '--exact-match', 'HEAD'], true)
            return releaseTagPattern.matcher(gitResult).matches()
        } catch (ExecException e) {
            return false
        }
    }

    def getCurrentVersion() {
        try{
            def result = gitExec(['describe', '--exact-match', 'HEAD'],true)
             new VersionNumber(result)
        } catch (ExecException e) {
            throw new RuntimeException("Not on a tag.")
        }
    }

    def getCurrentBranchName() {
        gitExec(['name-rev', '--name-only', 'HEAD'],true)
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
        def allReleaseTagNames = gitExec(['tag', '-l', tagSearchPattern])
        if(allReleaseTagNames){
            allReleaseTagNames.split('\n')
        }
    }

    def tag(String tag, String message) {
        println "tagging with $tag"
        gitExec(['tag', '-a', tag, '-m', message])
    }

    def pushTags() {
        println "pushing tags"
        gitExec(['push', '--tags'])
    }

    def gitExec(List gitArgs, boolean removeNewLines = false) {
        def stdout = new ByteArrayOutputStream()
        project.exec {
            executable = 'git'
            args = gitArgs
            standardOutput = stdout
        }
        if (stdout.toByteArray().length > 0) {
            def result = stdout.toString()
            if (removeNewLines) {
                stdout.toString().replaceAll("\\n", "")
            } else {
                result
            }
        } else {
            null
        }
    }
}