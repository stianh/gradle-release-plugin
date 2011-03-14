package no.entitas.gradle
import no.entitas.gradle.GitVersion
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task;

class GitReleasePlugin implements Plugin<Project> {

	def void apply(Project project) { 
		project.convention.plugins.gitRelease = new GitReleasePluginConvention()
		project.subprojects*.apply plugin: 'java'
		def gitVersion = new GitVersion(project)
		project.version = gitVersion

		project.task('cleanAll') << {}		
		Task cleanAllTask = project.tasks.getByName('cleanAll')
		cleanAllTask.dependsOn(project.subprojects*.clean)
		
		project.task('buildAll') << {}
		Task buildAll = project.tasks.getByName('buildAll')
		buildAll.dependsOn([cleanAllTask, project.subprojects*.build])
		
		project.task('releasePrepare') << {
	      gitVersion.releasePrepare()
		}
		Task releasePrepareTask = project.tasks.getByName('releasePrepare')
		releasePrepareTask.dependsOn(buildAll)
		
		project.task('releasePerform') << {
		  gitVersion.releasePerform()
		}
		Task performReleaseTask = project.tasks.getByName('releasePerform')
		performReleaseTask.dependsOn([releasePrepareTask,project.subprojects*.uploadArchives]) 	
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