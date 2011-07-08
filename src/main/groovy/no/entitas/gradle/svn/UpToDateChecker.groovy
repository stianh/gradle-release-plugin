package no.entitas.gradle.svn

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNStatusType
import org.tmatesoft.svn.core.wc.SVNClientManager

class UpToDateChecker implements ISVNStatusHandler {    
    private boolean remoteModifications=false;
        
    public boolean containsRemoteModifications(SVNClientManager svnClientManager, File path) {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        def statusClient=svnClientManager.getStatusClient()
        statusClient.doStatus(path, SVNRevision.HEAD, SVNDepth.INFINITY, true, true, true, false, this,null)
        return remoteModifications
    }
    
    public void handleStatus(SVNStatus status) throws SVNException {
        SVNStatusType statusType = status.getRemoteContentsStatus();
        if (statusType!=SVNStatusType.STATUS_NONE && statusType!=SVNStatusType.STATUS_NORMAL && statusType!=SVNStatusType.STATUS_IGNORED) {
            println("Remote modifications: "+status.getFile()+"\t"+statusType)
            remoteModifications=true;
        }
    }
}
