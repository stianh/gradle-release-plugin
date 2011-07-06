package no.entitas.gradle

import groovy.lang.Closure;

import org.gradle.api.Project;

class SvnReleasePlugin extends ReleasePlugin {
	def void setConvention(Project project) {
		project.convention.plugins.svnRelease = new SvnReleasePluginConvention();
	}
	
	def Version createVersion(Project project) {
		return new SvnVersion(project);
	}
	
	class SvnReleasePluginConvention {
		String snapshotDistributionUrl
		String releaseDistributionUrl

		def gitRelease(Closure closure) {
			closure.delegate = this
			closure()
		}
	}
}
