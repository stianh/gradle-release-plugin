package no.entitas.gradle

interface Version {
    def releasePrepare();
	def releasePerform();
}