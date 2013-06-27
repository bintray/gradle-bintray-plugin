package com.jfrog.bintray.gradle

import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.gradle.api.DefaultTask
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
    static final String API_URL_DEFAULT = 'https://api.bintray.com'

    //TODO: [by yl] Mark this as a publishing task

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
    String packageLicense

    @Input
    @Optional
    String[] packageLabels

    @Input
    @Optional
    String versionName

    @Input
    @Optional
    String versionDescriptor

    Artifact[] configurationUploads
    Artifact[] publicationUploads

    @TaskAction
    void bintrayUpload() {
        logger.info("Found {} configuration(s) to publish.", configurations?.length ?: 0);
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
            null
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
            null
        }.flatten() as Artifact[]

        //Upload the files
        HTTPBuilder http = createHttpClient()
        def repoPath = "${userOrg ?: user}/$repoName"
        def packagePath = "$repoPath/$packageName"

        def createPackage = {
            http.request(POST, JSON) {
                uri.path = "/packages/$repoPath"
                //TODO: [by yl] Add the license param once supported
                body = [name: packageName, desc: packageDesc, license: packageLicense, labels: packageLabels]

                response.success = { resp ->
                    logger.info("Created package $packageName.")
                }
                response.failure = { resp ->
                    logger.error("Could not create package $packageName: $resp.statusLine")
                }
            }
        }

        //Check package existence
        //TODO: [by yl] Change this to head once bintray supports anon resource head requests
        def checkAndCreatePackage = {
            def create = http.request(GET) {
                uri.path = "/packages/$packagePath"
                response.success = { resp ->
                    logger.debug("Package $packageName exists.")
                    false
                }
                response.'404' = { resp ->
                    logger.info("Package $packageName does not exist. Attempting to creating it...")
                    true
                }
            }
            if (create) {
                createPackage()
            }
        }

        def uploadArtifact = { artifact ->
            def uploadUri = "/content/$packagePath/${artifact.version}/${artifact.path}"
            artifact.file.withInputStream { is ->
                logger.info("Uploading to $apiUrl$uploadUri...")
                if (dryRun) {
                    logger.info("(Dry run) Uploaded to $apiUrl$uploadUri.")
                    return
                }
                http.request(PUT) {
                    uri.path = uploadUri
                    requestContentType: BINARY
                    response.success = { resp ->
                        logger.info("Uploaded to $apiUrl$uri.path.")
                    }
                    response.failure = { resp ->
                        logger.error("Could not upload to $apiUrl$uri.path: $resp.statusLine")
                    }
                }
            }
        }

        checkAndCreatePackage()
        configurationUploads.each { uploadArtifact it }
        publicationUploads.each { uploadArtifact it }
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

    private HTTPBuilder createHttpClient() {
        def http = new HTTPBuilder(apiUrl)
        http.auth.basic user, apiKey
        //Follow permanent redirects for PUTs
        http.client.setRedirectStrategy(new DefaultRedirectStrategy() {
            @Override
            boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
                def redirected = super.isRedirected(request, response, context)
                return redirected || response.getStatusLine().getStatusCode() == 301
            }

            @Override
            HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws org.apache.http.ProtocolException {
                URI uri = getLocationURI(request, response, context)
                String method = request.requestLine.method
                if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
                    return new HttpHead(uri)
                } else if (method.equalsIgnoreCase(HttpPut.METHOD_NAME)) {
                    return new HttpPut(uri)
                } else {
                    return new HttpGet(uri)
                }
            }
        })
        http
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
            (groupId?.replaceAll('\\.', '/') ?: "") + "/$name/$version/$name-$version" + (classifier ? "-$classifier" : "") +
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

    /*def createPackage() {
        // Credit for code goes to:
        //      https://github.com/bintray/bintray-examples/blob/master/gradle-example/build.gradle

        def repoPath = '/packages/' + source + '/' + repoName
        def http = new HTTPBuilder(apiBaseUrl)
        http.auth.basic username, apiKey
        http.request(GET, JSON) {
            uri.path = repoPath + '/' + packageName
            response.'404' = {
                http = new HTTPBuilder(apiBaseUrl)
                http.auth.basic username, apiKey
                http.request(POST, JSON) {
                    uri.path = repoPath
                    body = [name: packageName, desc: description, desc_url: descUrl, labels: tags]

                    response.success = { resp ->
                        println 'Created!!!'
                    }
                }
            }
        }
    }*/
}
