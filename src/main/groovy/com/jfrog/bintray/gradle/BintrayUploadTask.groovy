package com.jfrog.bintray.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

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

    File[] configurationFiles
    File[] publicationFiles

    @TaskAction
    void bintrayUpload() {
        //Extract from convention so that we don't need to use getters
        apiUrl = getApiUrl()
        user = getUser()
        apiKey = getApiKey()
        configurations = getConfigurations()
        publications = getPublications()
        userOrg = getUserOrg()
        repoName = getRepoName()
        packageName = getPackageName()
        packageDesc = getPackageDesc()
        packageLabels = getPackageLabels()

        logger.info("Found {} configuration(s) to publish.", configurations?.length ?: 0);
        configurations * {
            if (it instanceof CharSequence) {
                Configuration configuration = project.configurations.findByName(it)
                if (configuration != null) {
                    collectFiles(configuration)
                } else {
                    logger.error("{}: Could not find configuration: {}.", path, it)
                }
            } else if (conf instanceof Configuration) {
                configurationFiles = collectFiles((Configuration) it)
            } else {
                logger.error("{}: Unsupported configuration type: {}.", path, it.class)
            }
        }

        publications * {
            if (it instanceof CharSequence) {
                Publication publication = project.extensions.getByType(PublishingExtension).publications.findByName(it)
                if (publication != null) {
                    collectFiles(publication)
                } else {
                    logger.error("{}: Could not find publication: {}.", path, it);
                }
            } else if (conf instanceof Configuration) {
                publicationFiles = collectFiles((Configuration) it)
            } else {
                logger.error("{}: Unsupported publication type: {}.", path, it.class)
            }
        }
    }

    File[] collectFiles(Configuration config) {
        config.allArtifacts*.findResults {
            File file = it.getFile();
            if (!file.exists()) {
                logger.error("{}: file {} could not be found.", path, file.getAbsolutePath())
                return null
            }
            file
        }.unique();
    }

    File[] collectFiles(MavenPublication publication) {
        def files = publication.artifacts*.findResults {
            File file = it.getFile();
            if (!file.exists()) {
                logger.error("{}: file {} could not be found.", path, file.getAbsolutePath())
                return null
            }
            file
        }.unique()
        //files << mavenNormalizedPublication.getPomFile();
        files

        /*for (MavenPublication mavenPublication : mavenPublications) {
            String publicationName = mavenPublication.getName();
            if (!(mavenPublication instanceof MavenPublicationInternal)) {
                // TODO: Check how the descriptor file can be extracted without using asNormalisedPublication
                log.warn("Maven publication name '{}' is of unsupported type '{}'!",
                        publicationName, mavenPublication.getClass());
                continue;
            }
            MavenPublicationInternal mavenPublicationInternal = (MavenPublicationInternal) mavenPublication;
            MavenNormalizedPublication mavenNormalizedPublication = mavenPublicationInternal.asNormalisedPublication();
            MavenProjectIdentity projectIdentity = mavenNormalizedPublication.getProjectIdentity();

            // First adding the descriptor
            File file = mavenNormalizedPublication.getPomFile();
            DeployDetails.Builder builder = createBuilder(processedFiles, file, publicationName);
            if (builder != null) {
                PublishArtifactInfo artifactInfo = new PublishArtifactInfo(
                        projectIdentity.getArtifactId(), "pom", "pom", null, file);
                addMavenArtifactToDeployDetails(deployDetails, publicationName, projectIdentity, builder, artifactInfo);
            }
            MavenArtifactSet artifacts = mavenPublication.getArtifacts();
            for (MavenArtifact artifact : artifacts) {
                file = artifact.getFile();
                builder = createBuilder(processedFiles, file, publicationName);
                if (builder == null) continue;
                PublishArtifactInfo artifactInfo = new PublishArtifactInfo(
                        projectIdentity.getArtifactId(), artifact.getExtension(),
                        artifact.getExtension(), artifact.getClassifier(),
                        file);
                addMavenArtifactToDeployDetails(deployDetails, publicationName, projectIdentity, builder, artifactInfo);
            }
        }*/
    }

    /*
        public void publications(Object... publications) {
                if (publications == null) {
                    return;
                }
                for (Object publication : publications) {
                    if (publication instanceof CharSequence) {
                        Publication publicationObj = getProject().getExtensions()
                                .getByType(PublishingExtension.class).getPublications().findByName(publication.toString());
                        if (publicationObj != null) {
                            addPublication(publicationObj);
                        } else {
                            log.error("Publication named '{}' does not exist for project '{}' in task '{}'.",
                                    publication, getProject().getPath(), getPath());
                        }
                    } else if (publication instanceof Publication) {
                        addPublication((Publication) publication);
                    } else {
                        log.error("Publication type '{}' not supported in task '{}'.",
                                new Object[]{publication.getClass().getName(), getPath()});
                    }
                }
                publishPublicationsSpecified = true;
            }

            private void addPublication(Publication publicationObj) {
                if (publicationObj instanceof IvyPublication) {
                    ivyPublications.add((IvyPublication) publicationObj);
                } else if (publicationObj instanceof MavenPublication) {
                    mavenPublications.add((MavenPublication) publicationObj);
                } else {
                    log.warn("Publication named '{}' in project '{}' is of unknown type '{}'",
                            publicationObj.getName(), getProject().getPath(), publicationObj.getClass());
                }
            }

            public Set<IvyPublication> getIvyPublications() {
                return ivyPublications;
            }

            public Set<MavenPublication> getMavenPublications() {
                return mavenPublications;
            }
         */
}
