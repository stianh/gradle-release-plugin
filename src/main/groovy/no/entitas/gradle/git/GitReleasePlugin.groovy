package no.entitas.gradle.git

import no.entitas.gradle.ReleasePlugin
import no.entitas.gradle.Version
import org.gradle.api.Project

class GitReleasePlugin extends ReleasePlugin {
	def void setConvention(Project project) {
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