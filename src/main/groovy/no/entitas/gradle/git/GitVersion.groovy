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

import java.util.regex.Pattern
import no.entitas.gradle.Version
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.resources.MissingResourceException
import org.gradle.process.internal.ExecException

class GitVersion implements Version {
    Pattern releaseTagPattern = ~/^\S+-REL-\d+$/
    Project project
 
    String versionNumber
   

    def GitVersion(Project project) {
        this.project = project
        
        project.gradle.taskGraph.whenReady { graph ->
            setup(graph)
        }

    }

    def setup(graph) {
        project.logger.info("Setting version number...")

        if (graph.hasTask(':releasePrepare')) {
            releasePreConditions()
            versionNumber = getNextTagName()
            project.logger.info("  New version number for release build is ${versionNumber}")
        } else if (isOnReleaseTag()) {
            if (hasLocalModifications()) {  
                versionNumber = getCurrentBranchName() + '-SNAPSHOT'
                this.project.logger.info("  Version number for build on release tag with local modification is ${versionNumber}")
              }else {
                versionNumber = getCurrentVersion()
                project.logger.info("  Version number for build on release tag is ${versionNumber}")
            }
        } else {
            versionNumber = getCurrentBranchName() + '-SNAPSHOT'
            project.logger.info("  Version number for regular build is ${versionNumber}")
        }
    }

    // TODO Ensure that we are on a branch
    def releasePreConditions() {
       if (hasLocalModifications()) {
            throw new RuntimeException('Uncommitted changes found in the source tree:\n' + getLocalModifications())
        }

        if (isOnReleaseTag()) {
            throw new GradleException('No changes since last tag')
        }

        if (branchIsAheadOfRemote()) {
            throw new GradleException('Project contains unpushed commits');
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
            tagNameOnCurrentRevision()
        } catch (ExecException e) {
            throw new GradleException("Not on a tag.", e)
        }
    }

    def tagNameOnCurrentRevision() {
        gitExec(['describe', '--exact-match', 'HEAD'], true)
    }

    def tagNamesOnCurrentRevision() {
        def head = repository.getRef(Constants.HEAD)

        tagsKeyedOnCommitsObjectId.get(head.objectId)
    }

    
    def getCurrentBranchName() {
       def refName = gitExec(['symbolic-ref', '-q', 'HEAD'], true)
       if (!refName) {
            throw new RuntimeException('Could not find the current branch name');
        } else if (!refName.startsWith('refs/heads/')) {
            throw new RuntimeException('Checkout the branch to release from');
        }
        def prefixLength = 'refs/heads/'.length()
        def branchName = refName[prefixLength..-1]
    }

    def normalizeBranchName(String branchName) {
        branchName.replaceAll('[^\\w\\.\\-\\_]', '_');
    }

    def getNextTagName() {
        def currentBranch = getCurrentBranchName()
        def latestReleaseTag = getLatestReleaseTag(currentBranch)

        if (latestReleaseTag) {
            nextReleaseTag(latestReleaseTag)
        } else {
            def startVersion = project.extensions.getByName('release').startVersion.call(currentBranch)
            formatTagName(currentBranch, startVersion)
        }
    }

    def nextReleaseTag(String previousReleaseTag) {
        project.logger.info("previousReleaseTag ${previousReleaseTag}")
        def tagNameParts = previousReleaseTag.split('-').toList()
        def currentVersion = tagNameParts[-1]
        project.logger.info("currentVersion ${currentVersion}")
        def nextVersion = project.extensions.getByName('release').versionStrategy.call(currentVersion)
        project.logger.info("nextVersion ${nextVersion}")
        tagNameParts.pop()
        tagNameParts.pop()
        def branchName = tagNameParts.join('-')
        project.logger.info("branchName ${branchName}")
        formatTagName(branchName, nextVersion)
    }

    def formatTagName(branchName, version) {
        "$branchName-REL-$version"
    }

    /**
     * 1. Find all tags that matches the release tag pattern
     * 2. Create a RevTag instance for each of these tags
     * 3. Sort the RevTags on creation date, newest last
     * 4. Retrieve the tag name of the newest
     */
    def getLatestReleaseTag(String currentBranch) {
        def tagSearchPattern = "${currentBranch}-REL-*"
        gitExec(['for-each-ref', '--count=1', "--sort=-taggerdate",
            "--format=%(refname:short)", "refs/tags/${tagSearchPattern}"])
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

