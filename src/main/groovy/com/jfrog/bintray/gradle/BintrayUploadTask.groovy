package com.jfrog.bintray.gradle

import groovyx.net.http.HTTPBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Upload

import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

class BintrayUploadTask extends DefaultTask {

    static final String NAME = 'bintrayUpload'
    static final String GROUP = 'publishing'
    static final String DESCRIPTION = 'Publishes artifacts to bintray.com.'
    static final String API_URL_DEFAULT = 'https://api.bintray.com'

    @Input
    @Optional
    String apiUrl

    @Input
    String user

    @Input
    String apiKey

    @Input
    @Optional
    Object[] configurations

    @Input
    @Optional
    Object[] publications

    @Input
    boolean publish

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
    @Optional
    String packageDesc

    @Input
    @Optional
    String packageWebsiteUrl

    @Input
    @Optional
    String packageIssueTrackerUrl

    @Input
    @Optional
    String packageVcsUrl

    @Input
    @Optional
    String[] packageLicenses

    @Input
    @Optional
    String[] packageLabels

    @Input
    @Optional
    boolean packagePublicDownloadNumbers

    @Input
    @Optional
    String versionName

    @Input
    @Optional
    String versionDesc

    @Input
    @Optional
    String versionVcsTag

    Artifact[] configurationUploads
    Artifact[] publicationUploads

    {
        group = GROUP
        description = DESCRIPTION
    }

    @TaskAction
    void bintrayUpload() {
        logger.info("Found {} configuration(s) to publish.", configurations?.length ?: 0);
        //TODO: [by yl] replace with findResults for Gradle 2.x
        configurationUploads = configurations.collect {
            if (it instanceof CharSequence) {
                Configuration configuration = project.configurations.findByName(it)
                if (configuration != null) {
                    return collectArtifacts(configuration)
                } else {
                    logger.error("{}: Could not find configuration: {}.", path, it)
                }
            } else if (conf instanceof Configuration) {
                return collectArtifacts((Configuration) it)
            } else {
                logger.error("{}: Unsupported configuration type: {}.", path, it.class)
            }
            []
        }.flatten() as Artifact[]

        publicationUploads = publications.collect {
            if (it instanceof CharSequence) {
                Publication publication = project.extensions.getByType(PublishingExtension).publications.findByName(it)
                if (publication != null) {
                    return collectArtifacts(publication)
                } else {
                    logger.error("{}: Could not find publication: {}.", path, it);
                }
            } else if (conf instanceof MavenPublication) {
                return collectArtifacts((Configuration) it)
            } else {
                logger.error("{}: Unsupported publication type: {}.", path, it.class)
            }
            []
        }.flatten() as Artifact[]

        //Upload the files
        HTTPBuilder http = BintrayHttpClientFactory.create(apiUrl, user, apiKey)
        def repoPath = "${userOrg ?: user}/$repoName"
        def packagePath = "$repoPath/$packageName"

        def checkAndCreatePackage = {
            def create
            http.request(HEAD) {
                uri.path = "/packages/$packagePath"
                response.success = { resp ->
                    logger.debug("Package '$packageName' exists.")
                }
                response.'404' = { resp ->
                    logger.info("Package '$packageName' does not exist. Attempting to creating it...")
                    create = true
                }
            }
            if (create) {
                if (dryRun) {
                    logger.info("(Dry run) Created pakage '$packagePath'.")
                    return
                }
                http.request(POST, JSON) {
                    uri.path = "/packages/$repoPath"
                    body = [name: packageName, desc: packageDesc, licenses: packageLicenses, labels: packageLabels,
                            website_url: packageWebsiteUrl, issue_tracker_url: packageIssueTrackerUrl, vcs_url: packageVcsUrl,
                            public_download_numbers: packagePublicDownloadNumbers]

                    response.success = { resp ->
                        logger.info("Created package '$packagePath'.")
                    }
                    response.failure = { resp ->
                        throw new GradleException("Could not create package '$packagePath': $resp.statusLine")
                    }
                }
            }
        }

        def checkAndCreateVersion = {
            def create
            http.request(HEAD) {
                uri.path = "/packages/$packagePath/versions/$versionName"
                response.success = { resp ->
                    logger.debug("Version '$packagePath/$versionName' exists.")
                }
                response.'404' = { resp ->
                    logger.info("Version '$packagePath/$versionName' does not exist. Attempting to creating it...")
                    create = true
                }
            }
            if (create) {
                if (dryRun) {
                    logger.info("(Dry run) Created verion '$packagePath/$versionName'.")
                    return
                }
                http.request(POST, JSON) {
                    uri.path = "/packages/$packagePath/versions"
                    body = [name: versionName, desc: versionDesc, vcs_tag: versionVcsTag]
                    response.success = { resp ->
                        logger.info("Created version '$versionName'.")
                    }
                    response.failure = { resp ->
                        throw new GradleException("Could not create version '$versionName': $resp.statusLine")
                    }
                }
            }
        }

        def uploadArtifact = { artifact ->
            def uploadUri = "/content/$packagePath/${artifact.version}/${artifact.path}"
            artifact.file.withInputStream { is ->
                is.metaClass.totalBytes = {
                    artifact.file.length()
                }
                logger.info("Uploading to $apiUrl$uploadUri...")
                if (dryRun) {
                    logger.info("(Dry run) Uploaded to '$apiUrl$uploadUri'.")
                    return
                }
                http.request(PUT) {
                    uri.path = uploadUri
                    requestContentType = BINARY
                    body = is
                    response.success = { resp ->
                        logger.info("Uploaded to '$apiUrl$uri.path'.")
                    }
                    response.failure = { resp ->
                        throw new GradleException("Could not upload to '$apiUrl$uri.path': $resp.statusLine")
                    }
                }
            }
        }

        def publishVersion = {
            def publishUri = "/content/$packagePath/$versionName/publish"
            http.request(POST) {
                uri.path = publishUri
                response.success = { resp ->
                    logger.info("Published '$packagePath/$versionName'.")
                }
                response.failure = { resp ->
                    throw new GradleException("Could not publish '$packagePath/$versionName': $resp.statusLine")
                }
            }
        }

        checkAndCreatePackage()
        checkAndCreateVersion()
        configurationUploads.each { uploadArtifact it }
        publicationUploads.each { uploadArtifact it }
        if (publish) {
            publishVersion
        }
    }

    Artifact[] collectArtifacts(Configuration config) {
        def artifacts = config.allArtifacts.findResults {
            if (!it.file.exists()) {
                logger.error("{}: file {} could not be found.", path, it.file.getAbsolutePath())
                return null
            }
            new Artifact(
                    name: it.name, groupId: project.group, version: project.version, extension: it.extension,
                    type: it.type, classifier: it.classifier, file: it.file)
        }.unique();

        //Add pom file per config
        Upload installTask = project.tasks.withType(Upload).findByName('install');
        if (!installTask) {
            logger.info "maven plugin was not applied, no pom will be uploaded."
        } else {
            artifacts << new Artifact(name: project.name, groupId: project.group, version: project.version,
                    extension: 'pom', type: 'pom',
                    file: new File(getProject().convention.plugins['maven'].mavenPomDir, "pom-default.xml"))
        }
        artifacts
    }

    Artifact[] collectArtifacts(Publication publication) {
        if (!publication instanceof MavenPublication) {
            logger.info "{} can only use maven publications - skipping {}.", path, publication.name
            return []
        }
        def identity = publication.mavenProjectIdentity
        def artifacts = publication.artifacts.findResults {
            new Artifact(
                    name: identity.artifactId, groupId: identity.groupId, version: identity.version,
                    extension: it.extension, type: it.extension, classifier: it.classifier, file: it.file)
        }

        //Add the pom
        artifacts << new Artifact(
                name: identity.artifactId, groupId: identity.groupId, version: identity.version,
                extension: 'pom', type: 'pom', file: publication.asNormalisedPublication().pomFile)
        artifacts
    }

    static class Artifact {
        String name
        String groupId
        String version
        String extension
        String type
        String classifier
        File file

        def getPath() {
            (groupId?.replaceAll('\\.', '/') ?: "") + "/$name/$version/$name-$version" +
                    (classifier ? "-$classifier" : "") +
                    (extension ? ".$extension" : "")
        }

        boolean equals(o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            Artifact artifact = (Artifact) o

            if (classifier != artifact.classifier) {
                return false
            }
            if (extension != artifact.extension) {
                return false
            }
            if (file != artifact.file) {
                return false
            }
            if (groupId != artifact.groupId) {
                return false
            }
            if (name != artifact.name) {
                return false
            }
            if (type != artifact.type) {
                return false
            }
            if (version != artifact.version) {
                return false
            }

            return true
        }

        int hashCode() {
            int result
            result = name.hashCode()
            result = 31 * result + groupId.hashCode()
            result = 31 * result + version.hashCode()
            result = 31 * result + (extension != null ? extension.hashCode() : 0)
            result = 31 * result + (type != null ? type.hashCode() : 0)
            result = 31 * result + (classifier != null ? classifier.hashCode() : 0)
            result = 31 * result + file.hashCode()
            return result
        }
    }
}
