package com.jfrog.bintray.gradle.tasks

import com.jfrog.bintray.gradle.tasks.entities.Artifact
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.WorkResults

class RecordingCopyTask extends Copy {

    static String NAME = '_bintrayRecordingCopy'

    def fileUploads = []

    @Override
    @SkipWhenEmpty
    @Optional
    public File getDestinationDir() {
        project.getBuildDir()
    }

    @Override
    protected CopyAction createCopyAction() {
        def intoDir = project.relativePath(getRootSpec().getDestinationDir())
        // In case we're running on Windows, the path separator should be replaced.
        intoDir = intoDir.replace('\\', '/')
        return {
            CopyActionProcessingStream stream ->
                stream.process {
                    FileCopyDetailsInternal details ->
                        if (!details.isDirectory()) {
                            def destRelPath = intoDir != null ? (intoDir + '/' + details.getPath()) : details.getPath()
                            fileUploads << new Artifact(file: details.file, path: destRelPath)
                            didWork = true
                        }
                }
                WorkResults.didWork(true)
        }
    }
}