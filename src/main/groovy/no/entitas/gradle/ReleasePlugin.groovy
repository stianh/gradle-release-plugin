package no.entitas.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvableDependencies

abstract class ReleasePlugin implements Plugin<Project> {
    def TASK_RELEASE_PREPARE = 'releasePrepare'
    def TASK_RELEASE_PERFORM = 'releasePerform'

    def void apply(Project project) {
        def version = createVersion(project)
        project.version = version

        project.allprojects.each { currentProject ->
            currentProject.configurations.all {
                incoming.afterResolve { resolvableDependencies ->
                    ensureNoSnapshotDependencies(resolvableDependencies)
                }
            }
        }

        if (project.subprojects.isEmpty()) {
            Task releasePrepareTask = project.task(TASK_RELEASE_PREPARE) << {
                version.releasePrepare()
            }
            releasePrepareTask.dependsOn(project.tasks.build)

            Task performReleaseTask = project.task(TASK_RELEASE_PERFORM) << {
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

            Task releasePrepareTask = project.task(TASK_RELEASE_PREPARE) << {
                version.releasePrepare()
            }
            releasePrepareTask.dependsOn(buildAll)

            Task performReleaseTask = project.task(TASK_RELEASE_PERFORM) << {
                version.releasePerform()
            }
            performReleaseTask.dependsOn([releasePrepareTask, project.subprojects*.uploadArchives])
        }
    }

    def ensureNoSnapshotDependencies(ResolvableDependencies resolvableDependencies) {
        def deps = [] as Set

        resolvableDependencies.dependencies.each { Dependency dependency ->
            if (dependency.version?.contains('SNAPSHOT')) {
                deps.add("${dependency.group}:${dependency.name}:${dependency.version}")
            }
        }

        if (!deps.isEmpty()) {
            throw new IllegalStateException("Project contains SNAPSHOT dependencies: ${deps}")
        }
    }

    abstract def Version createVersion(Project project)
}