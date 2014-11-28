package com.jfrog.bintray.gradle

import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Upload
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

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
    @Optional
    CopySpec filesSpec

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
    Map packageAttributes

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
    String versionReleased

    @Input
    @Optional
    boolean signVersion

    @Input
    @Optional
    String gpgPassphrase

    @Input
    @Optional
    String versionVcsTag

    @Input
    @Optional
    Map versionAttributes

    @Input
    @Optional
    String ossUser

    @Input
    @Optional
    String ossPassword

    @Input
    @Optional
    String ossCloseRepo

    Artifact[] configurationUploads
    Artifact[] publicationUploads
    Artifact[] fileUploads

    boolean subtaskSkipPublish

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

        RecordingCopyTask recordingCopyTask = getDependsOn().find { it instanceof RecordingCopyTask }
        fileUploads = (recordingCopyTask ? recordingCopyTask.fileUploads : []) as Artifact[]

        //Upload the files
        HTTPBuilder http = BintrayHttpClientFactory.create(apiUrl, user, apiKey)
        def repoPath = "${userOrg ?: user}/$repoName"
        def packagePath = "$repoPath/$packageName"

        def setAttributes = { attributesPath, attributes, entity, entityName ->
            http.request(POST, JSON) {
                uri.path = attributesPath
                def builder = new JsonBuilder()
                builder.content = attributes.collect {
                    //Support both arrays and singular values - coerce to an array of values
                    ['name': it.key, 'values': [it.value].flatten()]
                }
                body = builder.toString()
                response.success = { resp ->
                    logger.info("Attributes set on $entity '$entityName'.")
                }
                response.failure = { resp, json ->
                    throw new GradleException(
                            "Could not set attributes on $entity '$entityName': $json.message")
                }
            }
        }

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
                    body = [name                   : packageName, desc: packageDesc, licenses: packageLicenses, labels: packageLabels,
                            website_url            : packageWebsiteUrl, issue_tracker_url: packageIssueTrackerUrl, vcs_url: packageVcsUrl,
                            public_download_numbers: packagePublicDownloadNumbers]

                    response.success = { resp ->
                        logger.info("Created package '$packagePath'.")
                    }
                    response.failure = { resp, json ->
                        throw new GradleException("Could not create package '$packagePath': $json.message")
                    }
                }
                if (packageAttributes) {
                    setAttributes "/packages/$packagePath/attributes", packageAttributes, 'package', packageName
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
                    versionReleased = Utils.toIsoDateFormat(versionReleased)
                    body = [name: versionName, desc: versionDesc, released: versionReleased, vcs_tag: versionVcsTag]
                    response.success = { resp ->
                        logger.info("Created version '$versionName'.")
                    }
                    response.failure = { resp, json ->
                        throw new GradleException("Could not create version '$versionName': $json.message")
                    }
                }
                if (versionAttributes) {
                    setAttributes "/packages/$packagePath/versions/$versionName/attributes", versionAttributes,
                            'version', versionName
                }
            }
        }

        def gpgSignVersion = {
            if (dryRun) {
                logger.info("(Dry run) Signed verion '$packagePath/$versionName'.")
                return
            }
            http.request(POST, JSON) {
                uri.path = "/gpg/$packagePath/versions/$versionName"
                if (gpgPassphrase != null) {
                    body = [passphrase: gpgPassphrase]
                }
                response.success = { resp ->
                    logger.info("Signed version '$versionName'.")
                }
                response.failure = { resp, json ->
                    throw new GradleException("Could not sign version '$versionName': $json.message")
                }
            }
        }

        def uploadArtifact = { artifact ->
            def versionPath = packagePath + '/' + versionName ?: artifact.version
            def uploadUri = "/content/$versionPath/${artifact.path}"
            if (!artifact.file.exists()) {
                logger.error("Skipping upload for missing file '$artifact.file'.")
                return
            }
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
            if (dryRun) {
                logger.info("(Dry run) Pulished verion '$packagePath/$versionName'.")
                return
            }
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

        def mavenCentralSync = {
            if (dryRun) {
                logger.info("(Dry run) Sync to Maven Central performed '$packagePath/$versionName'.")
                return
            }
            http.request(POST, JSON) {
                uri.path = "/maven_central_sync/$packagePath/versions/$versionName"
                body = [username: ossUser, password: ossPassword]
                if (ossCloseRepo != null) {
                    body << [close: ossCloseRepo]
                }
                response.success = { resp ->
                    logger.info("Sync to Maven Central performed for '$packagePath/$versionName'.")
                }
                response.failure = { resp, reader ->
                    throw new GradleException("Could not sync '$packagePath/$versionName' to Maven Central: $resp.statusLine $reader")
                }
            }
        }

        checkAndCreatePackage()
        checkAndCreateVersion()

        configurationUploads.each {
            uploadArtifact it
        }
        publicationUploads.each {
            uploadArtifact it
        }
        fileUploads.each {
            uploadArtifact it
        }
        if (signVersion) {
            gpgSignVersion()
        }
        if (publish && !subtaskSkipPublish) {
            publishVersion()
        }
        if (ossUser != null && ossPassword != null) {
            mavenCentralSync()
        }
    }

    Artifact[] collectArtifacts(Configuration config) {
        def pomArtifact
        def artifacts = config.allArtifacts.findResults {
            if (!it.file.exists()) {
                logger.error("{}: file {} could not be found.", path, it.file.getAbsolutePath())
                return null
            }
            pomArtifact = !pomArtifact && it.type == 'pom'
            new Artifact(
                    name: it.name, groupId: project.group, version: project.version, extension: it.extension,
                    type: it.type, classifier: it.classifier, file: it.file)
        }.unique();

        //Add pom file per config
        Upload installTask = project.tasks.withType(Upload).findByName('install');
        if (!installTask) {
            logger.info "maven plugin was not applied, no pom will be uploaded."
        } else if (!pomArtifact) {
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
}
