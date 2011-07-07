package no.entitas.gradle

import org.gradle.api.Project;
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNCopySource
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatus

class SvnVersion implements Version {
    private final Project project
    
    SvnVersion(Project project) {
        this.project=project;
    }
    
	def releasePrepare() {
        // TODO: Verify that nothing is uncommited
        // TODO: Verify that tag for this revision does not already exist
        // TODO: Calculate version-number by looking at matching tags
        // TODO: Perform build
	}
    
	def releasePerform() {
	    // TODO: Verify that nothing is uncommited
        
        def svnClientManager=createSVNClientManager();
        def svnStatus=getStatus(svnClientManager);
        
        // TODO: Verify that tag for this revision does not already exist

        // Calculate version-number by looking at matching tags
        
        def projectRootURL=getProjectRootURL(svnStatus);
        def svnRepo=getSVNRepository(projectRootURL);
        def nextVersionNumber=getNextVersionNumber(svnRepo);
        
        def branchName=getBranchName(svnStatus);
        println("Branch-name: "+branchName);
        def tagName=branchName+"-REL-"+nextVersionNumber;
        println("Tag to create: "+tagName);
        
        // Create tag
        createTag(svnClientManager, svnStatus, tagName);
    }
    
    private void printContentStatus(SVNStatus svnStatus) {
        println(svnStatus.getContentsStatus())
    }
    
    private SVNClientManager createSVNClientManager() {
        return SVNClientManager.newInstance();
    }
    
    private def SVNStatus getStatus(SVNClientManager svnClientManager) {
        return svnClientManager.getStatusClient().doStatus(project.rootDir,false)
    }    
    
    private String getProjectRootURL(SVNStatus svnStatus) {
        def url=svnStatus.URL.toDecodedString()
        def projRootURL=url.substring(0,url.lastIndexOf("/"));
        def branchName=url.substring(url.lastIndexOf("/")+1);
        if (projRootURL.endsWith("/branches")) {
            projRootURL=projRootURL.substring(0,projRootURL.lastIndexOf("/"));
        }
        return projRootURL;
    }
    
    private String getBranchName(SVNStatus svnStatus) {
        def url=svnStatus.URL.toDecodedString()
        return url.substring(url.lastIndexOf("/")+1);
    }
    
    private String getTagsURL(SVNStatus svnStatus) {    
        return getProjectRootURL(svnStatus)+"/tags";
    }
    
    private def SVNRepository getSVNRepository(String projectRootURL) {
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        return SVNRepositoryFactory.create(SVNURL.parseURIDecoded(projectRootURL) );
    }
    
    private def int getNextVersionNumber(SVNRepository repository) {
        def entries = repository.getDir( "tags", -1 , null , (Collection) null );
        def releaseTagPattern = ~/^\S+-REL-(\d+)$/
        def max=entries.max{it2->
            def matcher=releaseTagPattern.matcher(it2.name);
            if (matcher.matches()) {
              Integer.valueOf(matcher.group(1))
            } else {
              null
            }
        }
        def matcher=releaseTagPattern.matcher(max.name)
        int nextVersion=matcher.matches() ? Integer.valueOf(matcher.group(1))+1 : 1;
    }
    
    private def void createTag(SVNClientManager svnClientManager, SVNStatus svnStatus, String tagName) {
        def tagsUrl=getTagsURL(svnStatus);
        def rev = svnStatus.getRevision();
        def url = svnStatus.URL;
        def copyClient=svnClientManager.getCopyClient();
        def destURL=SVNURL.parseURIDecoded(tagsUrl+"/"+tagName);
        def copySrc=new SVNCopySource[1];
        copySrc[0]=new SVNCopySource(rev,rev,url)
        copyClient.doCopy(copySrc,destURL,false,false,true,"Tagging release "+tagName,null)
    }
}

