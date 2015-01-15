package com.jfrog.bintray.gradle

class Signature extends Artifact {
    Artifact signedArtifact

    @Override
    def getPath() {
        signedArtifact.path + "." + extension
    }

    boolean equals(o) {
        if (!super.equals(o)) {
            return false
        }

        Signature sig = (Signature) o

        if (signedArtifact != sig.signedArtifact) {
            return false
        }

        return true
    }

    int hashCode() {
        return 31 * super.hashCode() + (signedArtifact != null ? signedArtifact.hashCode() : 0)
    }
}
