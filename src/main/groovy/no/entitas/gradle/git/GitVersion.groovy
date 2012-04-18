/*
 * Copyright 2011- the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            this.project.logger.info("Setting version number...")

            if (graph.hasTask(':releasePrepare')) {
                releasePreConditions()
                versionNumber = getNextTagName()
                this.project.logger.info("  New version number for release build is ${versionNumber}")
            } else if (isOnReleaseTag()) {
                if (hasLocalModifications()) {
                    versionNumber = getCurrentBranchName() + '-SNAPSHOT'
                    this.project.logger.info("  Version number for build on release tag with local modification is ${versionNumber}")
                } else {
                    versionNumber = getCurrentVersion()
                    this.project.logger.info("  Version number for build on release tag is ${versionNumber}")
                }
            } else {
                versionNumber = getCurrentBranchName() + '-SNAPSHOT'
                this.project.logger.info("  Version number for regular build is ${versionNumber}")
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
        def branchName = gitExec(['name-rev', '--name-only', 'HEAD'], true)
        normalizeBranchName(branchName)
    }

    def normalizeBranchName(String branchName) {
        branchName.replaceAll('[^\\w\\.\\-\\_]', '_');
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