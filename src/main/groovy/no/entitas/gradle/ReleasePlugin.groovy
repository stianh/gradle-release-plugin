package no.entitas.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task

abstract class ReleasePlugin implements Plugin<Project> {
	def void apply(Project project) {
		def version = createVersion(project)
		project.version = version

        if (project.subprojects.isEmpty()) {
            Task releasePrepareTask = project.task('releasePrepare') << {
                version.releasePrepare()
            }
            releasePrepareTask.dependsOn(project.tasks.build)

            Task performReleaseTask = project.task('releasePerform') << {
                version.releasePerform()
            }
            performReleaseTask.dependsOn([releasePrepareTask, project.tasks.uploadArchives])
        } else {
            /*
            TODO: The subprojects closure configuration is not applied at the time when apply is called for this plugin.
                 The subprojects needs the java plugin at this time to resolve clean, build and the uploadArtifacts tasks.
                 Investigate if this somehow can be done lazy.
            */
            project.subprojects*.apply plugin: 'java'

            Task cleanAllTask = project.task('cleanAll') << {}
            cleanAllTask.dependsOn(project.subprojects*.clean)

            Task buildAll = project.task('buildAll') << {}
            buildAll.dependsOn([cleanAllTask, project.subprojects*.build])

            Task releasePrepareTask = project.task('releasePrepare') << {
                def dependencies = getSnapshotDependencies(project)

                if (!dependencies.isEmpty()) {
                    throw new RuntimeException('Project contains SNAPSHOT dependencies: ' + dependencies)
                }

                version.releasePrepare()
            }
            releasePrepareTask.dependsOn(buildAll)

            Task performReleaseTask = project.task('releasePerform') << {
                version.releasePerform()
            }
            performReleaseTask.dependsOn([releasePrepareTask, project.subprojects*.uploadArchives])
        }
    }

    def getSnapshotDependencies(def project) {
        def deps = [] as Set

        project.allprojects {
            it.configurations.all {
                it.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    def dependency = artifact.resolvedDependency

                    if (dependency.moduleVersion.contains("SNAPSHOT")) {
                        deps.add("${dependency.moduleGroup}:${dependency.moduleName}:${dependency.moduleVersion}")
                    }
                }
            }
        }

        deps
    }
	
	abstract def Version createVersion(Project project)
}