package com.jfrog.bintray.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Upload

class BintrayUploadTask extends DefaultTask {

    static final String NAME = 'bintrayUpload'
    static final String API_URL_DEFAULT = 'https://bintray.com/api'

    @Input
    @Optional
    String apiUrl

    @Input
    String user

    @Input
    String apiKey

    @Input
    Object[] configurations

    @Input
    Object[] publications

    @Input
    boolean dryRun

    @Input
    @Optional
    String userOrg

    @Input
    String repoName

    @Input
    String packageName

    @Input
    String packageDesc

    @Input
    String[] packageLabels

    File[] configurationUploads
    File[] publicationUploads

    @TaskAction
    void bintrayUpload() {
        logger.info("Found {} configuration(s) to publish.", configurations?.length ?: 0);
        configurationUploads = configurations.collect {
            if (it instanceof CharSequence) {
                Configuration configuration = project.configurations.findByName(it)
                if (configuration != null) {
                    return collectFiles(configuration)
                } else {
                    logger.error("{}: Could not find configuration: {}.", path, it)
                }
            } else if (conf instanceof Configuration) {
                return collectFiles((Configuration) it)
            } else {
                logger.error("{}: Unsupported configuration type: {}.", path, it.class)
            }
            null
        }.flatten() as File[]

        publicationUploads = publications.collect {
            if (it instanceof CharSequence) {
                Publication publication = project.extensions.getByType(PublishingExtension).publications.findByName(it)
                if (publication != null) {
                    return collectFiles(publication)
                } else {
                    logger.error("{}: Could not find publication: {}.", path, it);
                }
            } else if (conf instanceof MavenPublication) {
                return collectFiles((Configuration) it)
            } else {
                logger.error("{}: Unsupported publication type: {}.", path, it.class)
            }
            null
        }.flatten() as File[]
    }

    File[] collectFiles(Configuration config) {
        def files = config.allArtifacts.findResults {
            File file = it.getFile();
            if (!file.exists()) {
                logger.error("{}: file {} could not be found.", path, file.getAbsolutePath())
                return null
            }
            file
        }.unique();

        //Add pom file per config
        Upload installTask = project.tasks.withType(Upload).findByName('install');
        if (!installTask) {
            logger.warn "maven plugin is not applied, no pom will be uploaded."
        } else {
            files << new File(project.convention.plugins['maven'].mavenPomDir, "pom-default.xml")
        }
        files
    }

    File[] collectFiles(Publication publication) {
        if (!publication instanceof MavenPublication) {
            logger.warn "{} can only use maven publications - skipping {}.", path, publication.name
            return []
        }
        def files = publication.publishableFiles.files.findResults {
            if (!it.exists()) {
                logger.error("{}: file {} could not be found.", path, it.getAbsolutePath())
                return null
            }
            it
        }.unique()
        files
    }
}
