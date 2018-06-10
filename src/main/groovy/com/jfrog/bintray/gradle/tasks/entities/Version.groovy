package com.jfrog.bintray.gradle.tasks.entities

class Version {
    private String name
    private boolean created
    private boolean gpgSign
    private String gpgPassphrase
    private boolean publish
    private boolean  mavenCentralSync

    Version(String name, boolean gpgSign, String gpgPassphrase, boolean publish, boolean mavenCentralSync) {
        this.name = name
        this.gpgSign = gpgSign
        this.gpgPassphrase = gpgPassphrase
        this.publish = publish
        this.mavenCentralSync = mavenCentralSync
    }

    boolean isCreated() {
        return created
    }

    void setAsCreated() {
        this.created = true
    }

    boolean isGpgSign() {
        return gpgSign
    }

    String getGpgPassphrase() {
        return gpgPassphrase
    }

    boolean isPublish() {
        return publish
    }

    boolean isMavenCentralSync() {
        return mavenCentralSync
    }

    void merge(Version version) {
        if (version) {
            this.gpgSign = this.gpgSign || version.gpgSign
            this.publish = this.publish || version.publish
            this.mavenCentralSync = this.mavenCentralSync || version.mavenCentralSync
            this.gpgPassphrase = gpgPassphrase ?: this.gpgPassphrase
        }
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class || name != ((Version) o).name) {
            return false
        }
        return true
    }

    int hashCode() {
        name != null ? name.hashCode() : 0
    }
}