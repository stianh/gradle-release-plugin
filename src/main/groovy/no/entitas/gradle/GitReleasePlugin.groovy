package no.entitas.gradle
import no.entitas.gradle.GitVersion
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task;

class GitReleasePlugin implements Plugin<Project> {

	def void apply(Project project) { 
		project.convention.plugins.gitRelease = new GitReleasePluginConvention()
		def gitVersion = new GitVersion(project)
		project.version = gitVersion
		
		if(project.subprojects.isEmpty()){
			Task releasePrepareTask = project.task('releasePrepare') << {
	      		gitVersion.releasePrepare()
			}
			releasePrepareTask.dependsOn(project.build)
		
			Task performReleaseTask = project.task('releasePerform') << {
		  		gitVersion.releasePerform()
			}
			performReleaseTask.dependsOn([releasePrepareTask,project.uploadArchives]) 	
		}else{
			/*TODO: The subprojects closure configuration is not applied at the time when apply is called for this plugin.
			  The subprojects needs the java plugin at this time to resovle clean, build and the uploadArtifacts tasks.
			  Investigate if this some how can be done lazy.
			*/
			project.subprojects*.apply plugin: 'java'

			Task cleanAllTask = project.task('cleanAll') << {}		
			cleanAllTask.dependsOn(project.subprojects*.clean)
		
			Task buildAll = project.task('buildAll') << {}
			buildAll.dependsOn([cleanAllTask, project.subprojects*.build])
		
			Task releasePrepareTask = project.task('releasePrepare') << {
	      		gitVersion.releasePrepare()
			}
			releasePrepareTask.dependsOn(buildAll)
		
			Task performReleaseTask = project.task('releasePerform') << {
		  		gitVersion.releasePerform()
			}
			performReleaseTask.dependsOn([releasePrepareTask,project.subprojects*.uploadArchives]) 	
		}
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