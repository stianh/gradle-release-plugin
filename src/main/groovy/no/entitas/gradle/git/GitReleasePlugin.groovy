package no.entitas.gradle.git

import no.entitas.gradle.ReleasePlugin
import no.entitas.gradle.Version
import org.gradle.api.Project

class GitReleasePlugin extends ReleasePlugin {
	def Version createVersion(Project project) {
		return new GitVersion(project);
	}
}