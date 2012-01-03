package no.entitas.gradle.git

import no.entitas.gradle.Version
import no.entitas.gradle.VersionNumber
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

class GitVersion implements Version {
    Project project
    String versionNumber
    def releaseTagPattern = ~/^\S+-REL-\d+$/

    def GitVersion(project) {
        this.project = project

        project.gradle.taskGraph.whenReady { graph ->
            if (graph.hasTask(':releasePrepare')) {
                releasePreConditions()
                versionNumber = getNextTagName()
            } else if (isOnReleaseTag() && !hasLocalModifications()) {
                versionNumber = getCurrentVersion()
            } else {
                versionNumber = getCurrentBranchName() + '-SNAPSHOT'
            }
        }
    }

    // TODO Ensure that we are on a branch
    def releasePreConditions() {
        if (hasLocalModifications()) {
            throw new RuntimeException('Uncommited changes found in the source tree:\n' + getLocalModifications())
        }

        if (isOnReleaseTag()) {
            throw new RuntimeException('No changes since last tag.')
        }


        if (branchIsAheadOfRemote()) {
            throw new RuntimeException('Project contains unpushed commits');
        }
    }

    boolean branchIsAheadOfRemote() {
        gitExec(['status']).contains('Your branch is ahead of')
    }

    String toString() {
        versionNumber
    }

    def releasePrepare() {
        tag(versionNumber, "Release Tag: ${versionNumber}")
    }

    def releasePerform() {
        pushTags()
    }

    def getLocalModifications() {
        gitExec(['status', '--porcelain'])
    }

    def hasLocalModifications() {
        getLocalModifications() != null
    }

    def isOnReleaseTag() {
        try {
            releaseTagPattern.matcher(tagNameOnCurrentRevision()).matches()
        } catch (ExecException e) {
            false
        }
    }

    def getCurrentVersion() {
        try {
            new VersionNumber(tagNameOnCurrentRevision())
        } catch (ExecException e) {
            throw new RuntimeException("Not on a tag.")
        }
    }

    def tagNameOnCurrentRevision() {
        gitExec(['describe', '--exact-match', 'HEAD'], true)
    }

    def getCurrentBranchName() {
        gitExec(['name-rev', '--name-only', 'HEAD'], true)
    }

    def getNextTagName() {
        def currentBranch = getCurrentBranchName()
        def latestTagVersion = getLatestTag(currentBranch)

        if (latestTagVersion == null) {
            "$currentBranch-REL-1"
        } else {
            latestTagVersion.nextVersionTag()
        }
    }

    def getLatestTag(String currentBranch) {
        def tagSearchPattern = "${currentBranch}-REL-*"
        def tags = getTagNames(tagSearchPattern)

        if (tags != null) {
            def versionNumbers = tags.collect {new VersionNumber(it)}
            GroovyCollections.max(versionNumbers)
        }
    }

    def getTagNames(String tagSearchPattern) {
        def allReleaseTagNames = gitExec(['tag', '-l', tagSearchPattern])
        
        if (allReleaseTagNames) {
            allReleaseTagNames.split('\n')
        }
    }

    def tag(String tag, String message) {
        project.logger.info("tagging with $tag")
        gitExec(['tag', '-a', tag, '-m', message])
    }

    def pushTags() {
        project.logger.info("pushing tags")
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
                result.replaceAll("\\n", "")
            } else {
                result
            }
        } else {
            null
        }
    }
}