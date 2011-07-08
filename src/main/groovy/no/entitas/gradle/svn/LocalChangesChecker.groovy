package no.entitas.gradle.svn

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNStatusType
import org.tmatesoft.svn.core.wc.SVNClientManager

class LocalChangesChecker implements ISVNStatusHandler {
    private boolean localModifications=false
    
    public boolean containsLocalModifications(SVNClientManager svnClientManager, File path,SVNRevision headRev) {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        def statusClient=svnClientManager.getStatusClient()
        statusClient.doStatus(path, headRev, SVNDepth.INFINITY, true, true, true, false, this,null)
        return localModifications
    }
    
    public void handleStatus(SVNStatus status) throws SVNException {
        SVNStatusType statusType = status.getContentsStatus();
        if (statusType!=SVNStatusType.STATUS_NONE && statusType!=SVNStatusType.STATUS_NORMAL && statusType!=SVNStatusType.STATUS_IGNORED) {
            println("Local modifications: "+status.getFile()+"\t"+statusType)
            localModifications=true;
        }
    }
}
