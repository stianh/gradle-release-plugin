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
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.IndexDiff
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.resources.MissingResourceException

class GitVersion implements Version {
    Pattern releaseTagPattern = ~/^\S+-REL-\d+$/

    Project project
    Repository repository
    Map tagsKeyedOnCommitsObjectId
    String versionNumber
    Status status

    def GitVersion(Project project, Repository repository) {
        this.project = project
        this.repository = repository
        this.tagsKeyedOnCommitsObjectId = tagsKeyedOnCommitsObjectId(repository)
        this.status = workDirStatus(repository)

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
            if (!status.clean) {
                versionNumber = getCurrentBranchName() + '-SNAPSHOT'
                project.logger.info("  Version number for build on release tag with local modification is ${versionNumber}")
            } else {
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
        if (!status.clean) {
            throw new GradleException('Uncommitted changes found in the source tree:\n' + buildStatusText())
        }

        if (isOnReleaseTag()) {
            throw new GradleException('No changes since last tag')
        }

        if (branchIsAheadOfRemote()) {
            throw new GradleException('Project contains unpushed commits');
        }
    }

    boolean branchIsAheadOfRemote() {
        def status = BranchTrackingStatus.of(repository, repository.fullBranch)

        status.aheadCount != 0
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

    def workDirStatus(Repository repository) {
        def workingTreeIterator = new FileTreeIterator(repository)
        IndexDiff diff = new IndexDiff(repository, Constants.HEAD, workingTreeIterator)
        diff.diff()

        new Status(diff);
    }


    def buildStatusText() {
        """
        Files with modifications:
        ${status.changed}
        ${status.added}
        ${status.conflicting}
        ${status.missing}
        ${status.modified}
        ${status.removed}
        ${status.untracked}
        """
    }

    def isOnReleaseTag() {
        tagNamesOnCurrentRevision().any { String tagName ->
            tagName.matches(releaseTagPattern)
        }
    }

    def getCurrentVersion() {
        // TODO what if none is found? is it even possible?
        tagNamesOnCurrentRevision().find() { String tagName ->
            tagName.matches(releaseTagPattern)
        }
    }

    def tagNamesOnCurrentRevision() {
        def head = repository.getRef(Constants.HEAD)

        tagsKeyedOnCommitsObjectId.get(head.objectId)
    }

    def tagsKeyedOnCommitsObjectId(Repository repository) {
        def tagsKeyedOnCommitsObjectId = [:]

        repository.tags.each { tagEntry ->
            // TODO check for objectId == null? Happens if tag is not annotated
            def commitsObjectId = repository.peel(tagEntry.value).peeledObjectId
            Set set = tagsKeyedOnCommitsObjectId.get(commitsObjectId)

            if (set == null) {
                set = [] as Set
                tagsKeyedOnCommitsObjectId.put(commitsObjectId, set)
            }

            set.add(tagEntry.key)
        }

        tagsKeyedOnCommitsObjectId
    }

    def getCurrentBranchName() {
        def fullBranchName = repository.fullBranch

        if (!fullBranchName) {
            throw new MissingResourceException('Could not find the current branch name');
        } else if (!fullBranchName.startsWith('refs/heads/')) {
            throw new MissingResourceException('Checkout the branch to release from');
        }

        def branchName = Repository.shortenRefName(fullBranchName)
        normalizeBranchName(branchName)
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
        def tagNameParts = previousReleaseTag.split('-').toList()
        def currentVersion = tagNameParts[-1]
        def nextVersion = project.extensions.getByName('release').versionStrategy.call(currentVersion)

        tagNameParts.pop()
        tagNameParts.pop()
        def branchName = tagNameParts.join('-')

        formatTagName(branchName, nextVersion)
    }

    def formatTagName(branchName, version) {
        "$branchName-REL-$version"
    }

    def getLatestReleaseTag(String currentBranch) {
        def revWalk = new RevWalk(repository)

        /**
         * 1. Find all tags that matches the release tag pattern
         * 2. Create a RevTag instance for each of these tags
         * 3. Sort the RevTags on creation date, newest last
         * 4. Retrieve the tag name of the newest
         */
        try {
            repository.tags.findAll() { tagEntry ->
                tagEntry.value.name =~ /${currentBranch}-REL-*/
            }.collect { releaseTag ->
                revWalk.parseTag(releaseTag.value.objectId)
            }.sort { revTag ->
                revTag.taggerIdent.when
            }.tail().tagName
        } finally {
            revWalk.release()
        }
    }

    def tag(String tag, String message) {
        project.logger.info("tagging with $tag")
        // TODO log result?
        Git.wrap(repository).tag().setName(tag).setMessage(message).call()
    }

    def pushTags() {
        project.logger.info("pushing tags")
        // TODO log result?
        Git.wrap(repository).push().setPushTags().call()
    }
}