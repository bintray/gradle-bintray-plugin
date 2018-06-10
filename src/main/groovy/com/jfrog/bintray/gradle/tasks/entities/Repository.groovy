package com.jfrog.bintray.gradle.tasks.entities

import java.util.concurrent.ConcurrentHashMap

class Repository {
    private String name
    private ConcurrentHashMap<String, Package> packages = new ConcurrentHashMap<String, Package>()

    Repository(String name) {
        this.name = name
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class || name != ((Repository) o).name) {
            return false
        }
        return true
    }

    int hashCode() {
        name != null ? name.hashCode() : 0
    }
}
