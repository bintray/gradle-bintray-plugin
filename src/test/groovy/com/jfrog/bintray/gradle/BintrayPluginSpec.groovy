package com.jfrog.bintray.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static com.jfrog.bintray.gradle.BintrayUploadTask.*

class BintrayPluginSpec extends Specification {
    Project project

    def setup() {
        URL resource = getClass().getResource('/gradle/build.gradle')
        def projDir = new File(resource.toURI()).getParentFile()

        project = ProjectBuilder.builder().withName('project').withProjectDir(projDir).build()
        //project.setProperty('testUserName', 'user1')
    }

    def "no BintrayUpload tasks are registered by default"() {
        when: "plugin applied to project"
        project.evaluate()
        then: "there is a BintrayUpload tasks registered"
        project.tasks.withType(BintrayUploadTask)
    }

    def populateDsl() {
        when: "plugin applied to sub-project"
        Project childProject = ProjectBuilder.builder().withName('childProject').withParent(project).build()
        childProject.apply plugin: 'bintray'
        project.evaluate()
        //Notify evaluation listeners
        def gradle = project.getGradle()
        gradle.listenerManager.allListeners*.projectsEvaluated gradle
        BintrayUploadTask bintrayUploadTask = project.tasks.findByName(NAME)

        then: "project is properly configured with ${NAME} task"
        bintrayUploadTask.getTaskDependencies().values.find { BintrayUploadTask.isAssignableFrom it.class }
        //!bintrayUploadTask.publishConfigurations.isEmpty()
        API_URL_DEFAULT == bintrayUploadTask.apiUrl
        GROUP == bintrayUploadTask.group
        DESCRIPTION == bintrayUploadTask.description
        'landman' == bintrayUploadTask.user
        'key' == bintrayUploadTask.apiKey
        ['deployables'] == bintrayUploadTask.configurations
        ['mavenStuff'] == bintrayUploadTask.publications
        ['art3.txt'] == bintrayUploadTask.files.collect {it.name}
        bintrayUploadTask.dryRun
        'myrepo' == bintrayUploadTask.repoName
        'myorg' == bintrayUploadTask.userOrg
        'mypkg' == bintrayUploadTask.packageName
        'what a fantastic package indeed!' == bintrayUploadTask.packageDesc
        ['Apache-2.0'] == bintrayUploadTask.packageLicenses
        ['gear', 'gore', 'gorilla'] == bintrayUploadTask.packageLabels
    }

    def upload() {
        when: "Invoke the ${NAME} task"
        project.evaluate()
        //Notify evaluation listeners
        def gradle = project.getGradle()
        gradle.listenerManager.allListeners*.projectsEvaluated gradle
        BintrayUploadTask bintrayUploadTask = project.tasks.findByName(NAME)
        execute bintrayUploadTask
        def configurationUploadPaths = bintrayUploadTask.configurationUploads*.file.absolutePath
        def publicationUploadPaths = bintrayUploadTask.publicationUploads*.file.absolutePath
        def filesUploadPaths = bintrayUploadTask.fileUploads*.file.absolutePath

        then: "Uploaded artifact"
        2 == bintrayUploadTask.configurationUploads.length
        2 == bintrayUploadTask.publicationUploads.length

        def sep = '\\'+ File.separator
        configurationUploadPaths.grep(~/.*files${sep}art1.txt/)
        configurationUploadPaths.grep ~/.*build${sep}poms${sep}pom-default.xml/

        publicationUploadPaths.grep ~/.*files${sep}art2.txt/
        publicationUploadPaths.grep ~/.*build${sep}publications${sep}mavenStuff${sep}pom-default.xml/

        filesUploadPaths.grep ~/.*files${sep}art3.txt/
    }

    private void execute(Task task) {
        for (Task dep : task.taskDependencies.getDependencies(task)) {
            for (Action action : dep.actions) {
                action.execute(dep)
            }
        }
        for (Action action : task.actions) {
            action.execute(task)
        }
    }
}
