package no.entitas.gradle.svn

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNStatusType
import org.tmatesoft.svn.core.wc.SVNClientManager

class UpToDateChecker {    
    public boolean isUpToDate(SVNClientManager svnClientManager, File path) {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        def statusClient=svnClientManager.getStatusClient()
        def remoteStatus=statusClient.doStatus(path, true);
        def localStatus=statusClient.doStatus(path,false);
        def isUpToDate = remoteStatus.committedRevision == localStatus.revision
        if (!isUpToDate) {
            println("Not up-to-date: localRevision="+localStatus.revision+", remoteRevision="+remoteStatus.committedRevision)
        }
        return isUpToDate
    }
}
