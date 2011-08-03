package no.entitas.gradle.svn

import groovy.lang.Closure;

import no.entitas.gradle.ReleasePlugin;
import no.entitas.gradle.Version;

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

		def svnRelease(Closure closure) {
			closure.delegate = this
			closure()
		}
	}
}
