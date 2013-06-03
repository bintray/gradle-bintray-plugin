package com.jfrog.bintray.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild

class BintrayPlugin implements Plugin<Project> {

    private Project project

    public void apply(Project project) {
        this.project = project;
        def extension = project.extensions.create("bintray", BintrayExtension, project)
        extension.with {
            apiUrl = BintrayUploadTask.API_URL_DEFAULT
        }

        //Create and configure the task
        BintrayUploadTask bintrayUpload = project.tasks.create(BintrayUploadTask.NAME, BintrayUploadTask)
        bintrayUpload.dependsOn(GradleBuild)


        bintrayUpload.conventionMapping.with {
            apiUrl = { extension.apiUrl }
            user = { extension.user }
            apiKey = { extension.key }
            configurations = { extension.configurations }
            publications = { extension.publications }
            userOrg = { extension.pkg.userOrg }
            repoName = { extension.pkg.repo }
            packageName = { extension.pkg.name ?: extension.user }
            packageDesc = { extension.pkg.desc }
            packageLabels = { extension.pkg.labels }
        }
        bintrayUpload.dependsOn(GradleBuild)
    }
}