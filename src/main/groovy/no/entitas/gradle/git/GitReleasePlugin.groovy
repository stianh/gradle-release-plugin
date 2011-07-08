package no.entitas.gradle.git
import no.entitas.gradle.ReleasePlugin;
import no.entitas.gradle.Version;
import no.entitas.gradle.git.GitVersion;

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task;

class GitReleasePlugin extends ReleasePlugin {
	
	def void setConvention(Project project) {
        println("Project plugins is "+project.convention.plugins)
		project.convention.plugins.gitRelease = new GitReleasePluginConvention();
	}
	
	def Version createVersion(Project project) {
		return new GitVersion(project);
	}
	
	class GitReleasePluginConvention {
	    String snapshotDistributionUrl
		String releaseDistributionUrl

	    def gitRelease(Closure closure) {
	        closure.delegate = this
	        closure() 
	    }
	}
}