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
import org.tmatesoft.svn.core.wc.SVNInfo;
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
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();        
        SVNDebugLog.setDefaultLog(new NullSVNDebugLog())
        def svnClientManager=SVNClientManager.newInstance();
        def svnStatus=svnClientManager.getStatusClient().doStatus(project.rootDir,true)
        def repoInfo=getRepoInfo(svnClientManager,svnStatus)
        println("RepoInfo: "+repoInfo)
        checkUpToDateAndNoLocalModifications(svnClientManager,repoInfo)
        svnClientManager.dispose()
	}
    
	def releasePerform() {
	    SVNRepositoryFactoryImpl.setup();
	    FSRepositoryFactory.setup();        
        SVNDebugLog.setDefaultLog(new NullSVNDebugLog());
        def svnClientManager=SVNClientManager.newInstance();
        def svnStatus=svnClientManager.getStatusClient().doStatus(project.rootDir,true)
        def repoInfo=getRepoInfo(svnClientManager,svnStatus)
        println("RepoInfo: "+repoInfo)
        checkUpToDateAndNoLocalModifications(svnClientManager,repoInfo)
        
        def svnRepo=SVNRepositoryFactory.create(repoInfo.rootURL)
        
        def latestTag=getLatestTag(svnRepo, repoInfo.branchName)        
        def nextVersionNumber=getNextVersionNumber(latestTag)
        def tagName=repoInfo.branchName+"-REL-"+nextVersionNumber;
        
        createTag(svnClientManager, svnStatus, repoInfo, tagName);
        svnClientManager.dispose()
    }
    
    private void checkUpToDateAndNoLocalModifications(SVNClientManager svnClientManager, RepoInfo repoInfo) {
        def containsLocalModifications=new LocalChangesChecker().containsLocalModifications(svnClientManager, project.rootDir, repoInfo.headRev)
        
        if (containsLocalModifications) {
            throw new RuntimeException("Workspace contains local modifications.");
        }
        
        def containsRemoteModifications=new UpToDateChecker().containsRemoteModifications(svnClientManager, project.rootDir, repoInfo.headRev)
        if (containsRemoteModifications) {
            throw new RuntimeException("Workspace is not up-to-date.")
        }
    }
    
    private RepoInfo getRepoInfo(SVNClientManager svnClientManager, SVNStatus svnStatus) {
        def url=svnStatus.URL;
        def headRevision=getHeadRevision(url,svnClientManager);
        def pathTail=SVNPathUtil.tail(url.getPath())
        if ("trunk".equals(pathTail)) {
            def rootURL=url.removePathTail();
            def tagsURL=rootURL.appendPath("tags",false)
            return new RepoInfo(rootURL,"trunk",false,tagsURL,headRevision);
        } else if ("branches".equals(SVNPathUtil.tail(url.removePathTail()))) {
            def branchName=SVNPathUtil.tail(url.getPath())
            def rootURL=url.removePathTail().removePathTail();
            def tagsURL=rootURL.appendPath("tags",false)
            return new RepoInfo(rootURL,branchName,true,tagsURL,headRevision);
        } else {
            throw new RuntimeException("Illegal url: "+url.getPath()+". Must end with /trunk or /branches/<branchname>.")
        }
    }
    
    private SVNRevision getHeadRevision(SVNURL url,SVNClientManager svnClientManager) {
        def wcClient=svnClientManager.getWCClient();
        SVNInfo info = wcClient.doInfo(url, SVNRevision.HEAD, SVNRevision.HEAD);
        return info.getRevision();
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
        def rev = repoInfo.headRev
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
        private final SVNRevision headRev;
        
        RepoInfo(SVNURL rootURL, String branchName, boolean isBranch, SVNURL tagsURL, SVNRevision headRev) {
            this.rootURL=rootURL
            this.branchName=branchName
            this.isBranch=isBranch
            this.tagsURL=tagsURL
            this.headRev=headRev
        }
        
        def String toString() {
            return "rootURL="+rootURL+", "+"branchName="+branchName+", isBranch="+isBranch+", tagsURL="+tagsURL+", headRev="+headRev;
        }
    }
    
    
    static public void main(String...args) {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        def svnClientManager=SVNClientManager.newInstance();
        def svnStatus=svnClientManager.getStatusClient().doStatus(new File(args[0]),true)
        println(svnStatus)
        println(new SvnVersion(null).getHeadRevision(svnStatus.URL, svnClientManager))
    }
}

