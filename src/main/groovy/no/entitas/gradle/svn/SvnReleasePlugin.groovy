package no.entitas.gradle.svn

import no.entitas.gradle.ReleasePlugin
import no.entitas.gradle.Version
import org.gradle.api.Project

class SvnReleasePlugin extends ReleasePlugin {
	def Version createVersion(Project project) {
		return new SvnVersion(project);
	}
}
