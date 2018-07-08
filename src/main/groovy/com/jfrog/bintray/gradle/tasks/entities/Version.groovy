package com.jfrog.bintray.gradle.tasks.entities

import org.apache.commons.lang.StringUtils

class Version {
    private String name
    private boolean created
    private boolean gpgSign
    private String gpgPassphrase
    private boolean publish
    private boolean mavenCentralSync

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

    boolean shouldPerformPublish(boolean publish) {
        // If publish already occurred for this version
        // Or
        // If no publish is needed
        // don't perform publishing
        if (this.publish || !publish) {
            return false
        }
        this.publish = publish
        return true
    }

    boolean shouldPerformMavenSync(boolean mavenCentralSync) {
        // If maven Central Sync already occurred for this version
        // Or
        // If mavenCentralSync is false
        // don't perform maven central sync
        if (this.mavenCentralSync || !mavenCentralSync) {
            return false
        }
        this.mavenCentralSync = mavenCentralSync
        return true
    }

    boolean shouldGpgSign(boolean gpgSign) {
        // If signing of the version already occurred
        // Or
        // If no signing is required
        // return false
        if (this.gpgSign || !gpgSign) {
            return false
        }
        this.gpgSign = gpgSign
        return true
    }

    boolean equals(o) {
        if (!(o instanceof String)) {
            return false
        }

        String name = (String) o
        return StringUtils.equals(this.name, name)
    }

    int hashCode() {
        name != null ? name.hashCode() : 0
    }
}