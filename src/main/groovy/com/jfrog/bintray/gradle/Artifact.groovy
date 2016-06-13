package com.jfrog.bintray.gradle

class Artifact {
    String name
    String groupId
    String version
    String extension
    String type
    String classifier
    File file
    String path
    String signedExtension

    def getPath() {
        path ?: (groupId?.replaceAll('\\.', '/') ?: "") + "/$name/$version/$name-$version" +
                (classifier ? "-$classifier" : "") +
                (signedExtension ? ".$signedExtension" : "") +
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
        if (path != artifact.path) {
            return false
        }
        if (type != artifact.type) {
            return false
        }
        if (version != artifact.version) {
            return false
        }
        if (signedExtension != artifact.signedExtension) {
            return false
        }

        return true
    }

    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0)
        result = 31 * result + (version != null ? version.hashCode() : 0)
        result = 31 * result + (extension != null ? extension.hashCode() : 0)
        result = 31 * result + (type != null ? type.hashCode() : 0)
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0)
        result = 31 * result + (path != null ? path.hashCode() : 0)
        result = 31 * result + file.hashCode()
        result = 31 * result + (signedExtension != null ? signedExtension.hashCode() : 0)

        return result
    }
}