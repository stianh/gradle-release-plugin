package no.entitas.gradle.git

import no.entitas.gradle.ReleasePluginExtension
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectIdRef.PeeledNonTag
import org.eclipse.jgit.lib.ObjectIdRef.PeeledTag
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Ref.Storage
import org.eclipse.jgit.lib.Repository
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProgressEvent
import org.gradle.tooling.ProgressListener
import org.gradle.tooling.ResultHandler
import spock.lang.Specification

/**
 * Unit test of {@link GitVersion}
 *
 * @author Stig Kleppe-Jorgensen, 2012.09.30
 */
class GitVersionTest extends Specification {
    ObjectId ID_1 = ObjectId.fromString('84f25f6c357612b9b4ff818655d128ef0f62696e')
    ObjectId ID_2 = ObjectId.fromString('b7773d13356e2f9623a8e43aaa487ab003c495dd')
    ObjectId ID_3 = ObjectId.fromString('3cf221eaf446c56f72230a410bff6c87a95afda1')

    def 'should set SNAPSHOT version for build not on release tag'() {
        when:
        def gitVersion = createVersion('tag2', true, false)

        then:
        gitVersion.versionNumber == 'master-SNAPSHOT'
    }

    def 'should set SNAPSHOT version for build on release tag with local modifications'() {
        when:
        def gitVersion = createVersion('master-REL-1', false, false)

        then:
        gitVersion.versionNumber == 'master-SNAPSHOT'
    }

    def 'should set release version for build on release tag without local modifications'() {
        when:
        def gitVersion = createVersion('master-REL-1', true, false)

        then:
        gitVersion.versionNumber == 'master-REL-1'
    }

    def 'should set next release version for build with task release'() {
        when:
        def gitVersion = createVersion('master-REL-1', true, true)

        then:
        gitVersion.versionNumber == 'master-REL-2'
    }

    def 'test run gradle project'() {
        expect:
        def projectConnector =
            GradleConnector.newConnector().
                forProjectDirectory('/tmp/gradletest' as File).
                useInstallation('/usr/local/Cellar/gradle/1.1/libexec' as File).
                connect()

        try {
            projectConnector.newBuild().forTasks('clean').addProgressListener(new ProgressListener() {
                @Override
                void statusChanged(ProgressEvent progressEvent) {
                    println progressEvent.description
                }
            }).run(new ResultHandler<Void>() {
                @Override
                void onComplete(Void t) {
                    println 'Complete'
                }

                @Override
                void onFailure(GradleConnectionException e) {
                    println e
                }
            })
        } finally {
            projectConnector.close()
        }
    }

    def createVersion(String tagName, boolean clean, boolean hasTaskReleasePrepare) {
        def gitVersion = new GitVersion(mockForProject(), mockForRepository(tagName)) {
            @Override
            def workDirStatus(Repository repository) {
               null
            }

            @Override
            boolean branchIsAheadOfRemote() {
                false
            }

            @Override
            def getLatestReleaseTag(String currentBranch) {
                'master-REL-1'
            }
        }

        def mockStatus = Mock(Status)
        mockStatus.clean >> clean
        gitVersion.status = mockStatus

        gitVersion.setup(mockForTaskGraph(hasTaskReleasePrepare))
        gitVersion
    }

    Repository mockForRepository(def tagName) {
        Mock(Repository).with { repository ->
            repository.getRef(Constants.HEAD) >> new PeeledNonTag(Storage.NEW, 'head', ID_2)
            repository.tags >> ['tag1' : tagRef('ref/tags/tag1'), "${tagName}" : tagRef("ref/tags/${tagName}")]
            repository.peel(_) >> { Ref ref -> ref }
            repository.fullBranch >> 'refs/heads/master'
            repository.newObjectReader() >> Mock(ObjectReader)
            repository
        }
    }

    PeeledTag tagRef(String tag) {
        new PeeledTag(Storage.NEW, tag, ID_3, ID_1)
    }

    Project mockForProject() {
        Mock(Project).with { project ->
            project.gradle >> mockForGradle()
            project.logger >> Mock(Logger)
            project.extensions >> mockForExtensionContainer()
            project
        }
    }

    def mockForExtensionContainer() {
        Mock(ExtensionContainer).with { ec ->
            ec.getByName(_) >> new ReleasePluginExtension()
            ec
        }
    }

    Gradle mockForGradle() {
        Mock(Gradle).with { gradle ->
            gradle.taskGraph >> Mock(TaskExecutionGraph)
            gradle
        }
    }

    TaskExecutionGraph mockForTaskGraph(boolean hasTaskReleasePrepare) {
        Mock(TaskExecutionGraph).with { taskGraph ->
            taskGraph.hasTask(_ as String) >> hasTaskReleasePrepare
            taskGraph
        }
    }
}
