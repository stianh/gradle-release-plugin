package no.entitas.gradle.svn

import no.entitas.gradle.Version
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.internal.util.SVNPathUtil
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNCopySource
import org.tmatesoft.svn.core.wc.SVNInfo
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.util.SVNDebugLog

class SvnVersion implements Version {
    private final def releaseTagPattern = ~/^(\S+)-REL-(\d+)$/
    private final Project project
    private final SVNStatus svnStatus;
    private final RepoInfo repoInfo
    private final String tagName
    private Boolean release;
    private String versionNumber;
    
    SvnVersion(Project project) {
        this.project=project;
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        DAVRepositoryFactory.setup();
        SVNDebugLog.setDefaultLog(new NullSVNDebugLog(project));
        def svnClientManager=SVNClientManager.newInstance();
        this.svnStatus=svnClientManager.getStatusClient().doStatus(project.rootDir,false)
        this.repoInfo=getRepoInfo(svnClientManager,svnStatus)
        //println("RepoInfo: "+repoInfo)
        
        def svnRepo=SVNRepositoryFactory.create(repoInfo.rootURL)
        
        def latestTag=getLatestTag(svnRepo, repoInfo.branchName)
        def nextVersionNumber=getNextVersionNumber(latestTag)
        this.tagName=repoInfo.branchName+"-REL-"+nextVersionNumber;
        svnClientManager.dispose()
        
        project.gradle.taskGraph.whenReady {graph ->
            if (graph.hasTask(':releasePrepare')) {
                checkUpToDateAndNoLocalModifications(svnClientManager,repoInfo)
                this.release=true;
                this.versionNumber = tagName
            } else {
                this.release=false;
                this.versionNumber = repoInfo.branchName + '-SNAPSHOT'
            }
        }
    }
    
    def String toString() {
        return versionNumber;
    }
    
	def releasePrepare() {
        project.logger.debug("RepoInfo: $repoInfo")
        project.logger.info("Tag to create: $tagName")
	}
    
	def releasePerform() {
        def svnClientManager=SVNClientManager.newInstance();
        checkUpToDateAndNoLocalModifications(svnClientManager,repoInfo)
        createTag(svnClientManager, svnStatus, repoInfo, tagName);
        svnClientManager.dispose()
    }

    private void checkUpToDateAndNoLocalModifications(SVNClientManager svnClientManager, RepoInfo repoInfo) {
        def containsLocalModifications = new LocalChangesChecker(project).
                containsLocalModifications(svnClientManager, project.rootDir, repoInfo.headRev)

        if (containsLocalModifications) {
            throw new IllegalStateException("Workspace contains local modifications.");
        }

        def containsRemoteModifications = new UpToDateChecker(project).
                containsRemoteModifications(svnClientManager, project.rootDir, repoInfo.headRev)

        if (containsRemoteModifications) {
            throw new IllegalStateException("Workspace is not up-to-date.")
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
        } else if ("branches".equals(SVNPathUtil.tail(url.removePathTail().getPath()))) {
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
        
        project.logger.info("Tagging release: $tagName")
        def dirsToMake=new SVNURL[1];
        dirsToMake[0]=destURL;
        def copyClient=svnClientManager.getCopyClient()        
        copyClient.doCopy(copySrc,destURL,false,false,true,"Tagging release "+tagName+", (from "+url+", rev "+rev,null)
    }
    
    public boolean isRelease() {
        if (release == null) {
            throw new GradleException("Can't determine whether this is a release build before the task graph is populated")
        }
        return release
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
}