package com.jfrog.bintray.gradle.tasks.entities

import java.util.concurrent.ConcurrentHashMap

class Package {
    private String name
    private boolean created
    private ConcurrentHashMap<String, Version> versions = new ConcurrentHashMap<String, Version>()

    Package(String name) {
        this.name = name
    }

    boolean isCreated() {
        return created
    }

    void setAsCreated() {
        this.created = true
    }

    public Version addVersionIfAbsent(Version version) {
        Version v = versions.putIfAbsent(version.name, version)
        if (v) {
            v.merge(version)
        }
        return v
    }

    // Returns the version of the Map if exists
    // Null if there is no such version in the map
    Version getVersionIfExists(Version v) {
        return versions.get(v)
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class || name != ((Package) o).name) {
            return false
        }
        return true
    }

    int hashCode() {
        name != null ? name.hashCode() : 0
    }
}