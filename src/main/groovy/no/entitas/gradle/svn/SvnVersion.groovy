package no.entitas.gradle.svn

import no.entitas.gradle.Version;

import org.gradle.api.Project;
import org.gradle.tooling.BuildException;
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.admin.ISVNEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNCopySource
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNEvent
import org.tmatesoft.svn.util.SVNDebugLog
import org.tmatesoft.svn.core.internal.util.SVNPathUtil

class SvnVersion implements Version {
    private final def releaseTagPattern = ~/^(\S+)-REL-(\d+)$/
    private final Project project
    
    SvnVersion(Project project) {
        this.project=project;
    }
    
	def releasePrepare() {
        SVNDebugLog.setDefaultLog(new NullSVNDebugLog())
        def svnClientManager=SVNClientManager.newInstance();
        checkUpToDateAndNoLocalModifications(svnClientManager)
        def svnStatus=svnClientManager.getStatusClient().doStatus(project.rootDir,false)
        def repoInfo=getRepoInfo(svnStatus)
        println("RepoInfo: "+repoInfo)
        svnClientManager.dispose()
	}
    
	def releasePerform() {
        SVNDebugLog.setDefaultLog(new NullSVNDebugLog());
        def svnClientManager=SVNClientManager.newInstance();
        checkUpToDateAndNoLocalModifications(svnClientManager)
        def svnStatus=svnClientManager.getStatusClient().doStatus(project.rootDir,false)
        def repoInfo=getRepoInfo(svnStatus)
        println("RepoInfo: "+repoInfo)
        
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();        
        def svnRepo=SVNRepositoryFactory.create(repoInfo.rootURL)
        
        def latestTag=getLatestTag(svnRepo, repoInfo.branchName)        
        def nextVersionNumber=getNextVersionNumber(latestTag)
        def tagName=repoInfo.branchName+"-REL-"+nextVersionNumber;
        
        createTag(svnClientManager, svnStatus, repoInfo, tagName);
        svnClientManager.dispose()
    }
    
    private void checkUpToDateAndNoLocalModifications(SVNClientManager svnClientManager) {
        def containsLocalModifications=new LocalChangesChecker().containsLocalModifications(svnClientManager, project.rootDir)
        
        if (containsLocalModifications) {
            throw new RuntimeException("Workspace contains local modifications.");
        }
        
        def containsRemoteModifications=new UpToDateChecker().containsRemoteModifications(svnClientManager, project.rootDir)
        if (containsRemoteModifications) {
            throw new RuntimeException("Workspace is not up-to-date.")
        }
    }
    
    private RepoInfo getRepoInfo(SVNStatus svnStatus) {
        def url=svnStatus.URL;
        def pathTail=SVNPathUtil.tail(url.getPath())
        if ("trunk".equals(pathTail)) {
            def rootURL=url.removePathTail();
            def tagsURL=rootURL.appendPath("tags",false)
            return new RepoInfo(rootURL,"trunk",false,tagsURL,svnStatus.getCommittedRevision());
        } else if ("branches".equals(SVNPathUtil.tail(url.removePathTail()))) {
            def branchName=SVNPathUtil.tail(url.getPath())
            def rootURL=url.removePathTail().removePathTail();
            def tagsURL=rootURL.appendPath("tags",false)
            return new RepoInfo(rootURL,branchName,true,tagsURL,svnStatus.getCommittedRevision());
        } else {
            throw new RuntimeException("Illegal url: "+url.getPath()+". Must end with /trunk or /branches/<branchname>.")
        }
    }
    
    private def SVNDirEntry getLatestTag(SVNRepository svnRepository, String branchName) {
        def entries = svnRepository.getDir( "tags", -1 , null , (Collection) null );
        SVNDirEntry max=entries.max{it2->
            def matcher=releaseTagPattern.matcher(it2.name);
            if (matcher.matches() && branchName.equals(matcher.group(1))) {
              Integer.valueOf(matcher.group(2))
            } else {
              null
            }
        }
    }
    
    private def int getNextVersionNumber(SVNDirEntry latestTag) {
        if (latestTag==null) {
            return 1;
        }        
        def matcher=releaseTagPattern.matcher(latestTag.name)
        return matcher.matches() ? Integer.valueOf(matcher.group(2))+1 : 1;
    }
    
    private def void createTag(SVNClientManager svnClientManager, SVNStatus svnStatus, RepoInfo repoInfo, String tagName) {
        def tagsUrl=repoInfo.tagsURL
        def rev = repoInfo.committedRevision
        def url = svnStatus.URL;
        def destURL=tagsUrl.appendPath(tagName,false);
        def copySrc=new SVNCopySource[1];
        copySrc[0]=new SVNCopySource(rev,rev,url)
        
        println("Tagging release: "+tagName)
        def dirsToMake=new SVNURL[1];
        dirsToMake[0]=destURL;
        def copyClient=svnClientManager.getCopyClient()        
        copyClient.doCopy(copySrc,destURL,false,false,true,"Tagging release "+tagName+", (from "+url+", rev "+rev,null)
    }
    
    private class RepoInfo {
        private final SVNURL rootURL;
        private final String branchName;
        private final boolean isBranch;
        private final SVNURL tagsURL;
        private final SVNRevision committedRevision;
        
        RepoInfo(SVNURL rootURL, String branchName, boolean isBranch, SVNURL tagsURL, SVNRevision committedRevision) {
            this.rootURL=rootURL
            this.branchName=branchName
            this.isBranch=isBranch
            this.tagsURL=tagsURL
            this.committedRevision=committedRevision
        }
        
        def String toString() {
            return "rootURL="+rootURL+", "+"branchName="+branchName+", isBranch="+isBranch+", tagsURL="+tagsURL+", committedRevision="+committedRevision;
        }
    }
}

