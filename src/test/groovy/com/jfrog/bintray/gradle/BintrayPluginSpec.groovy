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

        project = ProjectBuilder.builder().withProjectDir(projDir).build()
        //project.setProperty('testUserName', 'user1')
    }


    def "no BintrayUpload tasks are registered by default"() {
        when: "plugin applied to project"
        project.evaluate()
        then: "there is a BintrayUpload tasks registered"
        project.tasks.withType(BintrayUploadTask)
    }

    def populateDsl() {
        when: "plugin applied to project"
        project.evaluate()
        //Notify evaluation listeners
        def gradle = project.getGradle()
        gradle.listenerManager.allListeners*.projectsEvaluated gradle

        then: "project is properly configured with ${NAME} task"
        BintrayUploadTask bintrayUploadTask = project.tasks.findByName(NAME)
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

    /* def "lessCompileSite task is registered when site plugin is present"() {
         when: "the less and site plugins are applied to a project"
         project.apply(plugin: com.jfrog.bintray.gradle.bintray.BintrayPlugin)
         project.apply(plugin: SitePlugin)

         then: "the lessCompile task is registered with the project with appropriate configuration"
         project.tasks.getByName('lessCompileSite') instanceof LessCompile
         lessCompileSiteTask.description == 'Compiles the site LESS files to CSS.'
         lessCompileSiteTask.group == null
         'lessCompileSite' in publishSiteTask.dependsOn
         lessCompileSiteTask.sourceDir == project.file('src/site/less')
         lessCompileSiteTask.destinationDir == project.file('build/resources/site/css')
         lessCompileSiteTask.compress == null
         lessCompileSiteTask.encoding == null
     }

     def "lessCompileSite task defaults to site extension values"() {
         when: "the less and site plugins are applied to a project"
         project.apply(plugin: com.jfrog.bintray.gradle.bintray.BintrayPlugin)
         project.apply(plugin: SitePlugin)

         and: "the site extension is configured with non-default values"
         // Source dir isn't currently configurable at the site extension
         //project.site.sourceDir = 'site'
         project.site.sourceSet.output.resourcesDir = 'target'

         then: "the lessCompile task bases its configuration on the site extension values"
         //lessCompileSiteTask.sourceDir == project.file('less')
         lessCompileSiteTask.destinationDir == project.file('target/css')
     }

     def "direct configuration of lessCompileSite overrides the site extension values"() {
         when: "the less and site plugins are applied to a project"
         project.apply(plugin: com.jfrog.bintray.gradle.bintray.BintrayPlugin)
         project.apply(plugin: SitePlugin)

         and: "the site extension is configured with non-default values"
         project.site.sourceDir = 'site'
         project.site.outputDir = 'target'

         and: "the task is configured with non-default values"
         lessCompileSiteTask.sourceDir = 'lessSrc'
         lessCompileSiteTask.destinationDir = 'lessDest'

         then: "the lessCompile task bases its configuration on the task values"
         lessCompileSiteTask.sourceDir == project.file('lessSrc')
         lessCompileSiteTask.destinationDir == project.file('lessDest')
     }

     private LessCompile getLessCompileSiteTask() {
         return project.tasks.getByName('lessCompileSite') as LessCompile
     }

     private PublishSite getPublishSiteTask() {
         return project.tasks.getByName('publishSite') as PublishSite
     }*/
}
