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
