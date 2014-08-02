package com.jfrog.bintray.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

import static com.jfrog.bintray.gradle.BintrayUploadTask.*

class BintrayPluginIntegrationSpec extends Specification {

    Project project

    //TODO: [by yl] Upload a parameterized build and check the results using the REST API

    def setup() {
        URL resource = getClass().getResource('/gradle/parameterized-build.gradle')
        def projDir = new File(resource.toURI()).getParentFile()

        project = ProjectBuilder.builder().withName('project').withProjectDir(projDir).build()
        //project.setProperty('testUserName', 'user1')


        def username = project.hasProperty('bintrayUser') ? project.bintrayUser : System.getenv()['BINTRAY_USER']
        def password = project.hasProperty('bintrayKey') ? project.bintrayKey : System.getenv()['BINTRAY_KEY']


        def bt = BintrayHttpClientFactory.create(API_URL_DEFAULT, username, password)
        //WIP
    }

    /*
  String[] configurations

  String[] publications

  boolean publish


   */

    @Unroll
    def "build permutations"(String[] configurations, String[] publications, boolean publish, String userOrg) {
        //String packageDesc, String[] packageLicenses, String[] packageLabels, String versionName, String versionDesc String versionVcsTag

        //TODO: [by yl] Use rest to check the bintray-demo, bintray-demos repo under different publish options based on params
        /*expect:
        Math.max(a, b) == c

        where:
        a | b | c
        1 | 3 | 3
        7 | 4 | 4
        0 | 0 | 0*/

        /*
        where:
a << [3, 7, 0]
b << [5, 0, 0]
c << [5, 7, 0]
         */
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
        childProject.apply plugin: 'com.jfrog.bintray'
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

        then: "Uploaded artifact"
        2 == bintrayUploadTask.configurationUploads.length
        2 == bintrayUploadTask.publicationUploads.length

        configurationUploadPaths.grep(~/.*files\/art1.txt/)
        configurationUploadPaths.grep ~/.*build\/poms\/pom-default.xml/

        publicationUploadPaths.grep ~/.*files\/art2.txt/
        publicationUploadPaths.grep ~/.*build\/publications\/mavenStuff\/pom-default.xml/
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
