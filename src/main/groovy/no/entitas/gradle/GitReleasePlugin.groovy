package no.entitas.gradle
import no.nos.gradle.GitVersion
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task;

class GitReleasePlugin implements Plugin<Project> {

	def void apply(Project project) { 
		project.subprojects*.apply plugin: 'java'
		def ver = new GitVersion(project)
		project.version = ver

		project.task('cleanAll') << {}		
		Task cleanAllTask = project.tasks.getByName('cleanAll')
		cleanAllTask.dependsOn(project.subprojects*.clean)
		
		project.task('buildAll') << {}
		Task buildAll = project.tasks.getByName('buildAll')
		buildAll.dependsOn([cleanAllTask, project.subprojects*.build])
		
		project.task('releasePrepare') << {
	      ver.releasePrepare()
		}
		Task releasePrepareTask = project.tasks.getByName('releasePrepare')
		releasePrepareTask.dependsOn(buildAll)
		
		project.task('releasePerform') << {
		  ver.releasePerform()
		}
		Task performReleaseTask = project.tasks.getByName('releasePerform')
		performReleaseTask.dependsOn(releasePrepareTask) 	
	}
}