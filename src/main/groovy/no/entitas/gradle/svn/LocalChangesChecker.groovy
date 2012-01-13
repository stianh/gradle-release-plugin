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
package no.entitas.gradle.svn

import org.gradle.api.Project
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.wc.ISVNStatusHandler
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNStatusType

class LocalChangesChecker implements ISVNStatusHandler {
    boolean localModifications = false
    Project project

    LocalChangesChecker(Project project) {
        this.project = project
    }

    public boolean containsLocalModifications(SVNClientManager svnClientManager, File path,SVNRevision headRev) {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        def statusClient=svnClientManager.getStatusClient()
        statusClient.doStatus(path, headRev, SVNDepth.INFINITY, true, true, true, false, this,null)
        return localModifications
    }
    
    public void handleStatus(SVNStatus status) throws SVNException {
        SVNStatusType statusType = status.getContentsStatus();

        if (statusType != SVNStatusType.STATUS_NONE &&
                statusType != SVNStatusType.STATUS_NORMAL && 
                statusType != SVNStatusType.STATUS_IGNORED) {
            project.logger.debug("Local modifications: $status.file\t$statusType")
            localModifications = true;
        }
    }
}
