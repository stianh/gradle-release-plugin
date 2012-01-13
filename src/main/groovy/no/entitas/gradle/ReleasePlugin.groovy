/*
 * Copyright 2011- the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.entitas.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.execution.TaskExecutionGraph

abstract class ReleasePlugin implements Plugin<Project> {
    def TASK_RELEASE_PREPARE = 'releasePrepare'
    def TASK_RELEASE_PERFORM = 'releasePerform'

    def void apply(Project project) {
        def version = createVersion(project)
        project.version = version
        project.extensions.release = new ReleasePluginExtension()

        if (project.release.failOnSnapshotDependencies) {
            project.allprojects.each { currentProject ->
                currentProject.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if (taskGraph.hasTask(TASK_RELEASE_PREPARE)) {
                        currentProject.configurations.all.each { configuration ->
                            project.logger.info(
                                "Checking for snapshot dependencies in $currentProject.path -> $configuration.name")
                            ensureNoSnapshotDependencies(configuration)
                        }
                    }
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

    def ensureNoSnapshotDependencies(Configuration configuration) {
        def snapshotDependencies = [] as Set

        configuration.allDependencies.each { Dependency dependency ->
            if (dependency.version?.contains('SNAPSHOT')) {
                snapshotDependencies.add("${dependency.group}:${dependency.name}:${dependency.version}")
            }
        }

        if (!snapshotDependencies.isEmpty()) {
            throw new IllegalStateException("Project contains SNAPSHOT dependencies: ${snapshotDependencies}")
        }
    }

    abstract def Version createVersion(Project project)
}