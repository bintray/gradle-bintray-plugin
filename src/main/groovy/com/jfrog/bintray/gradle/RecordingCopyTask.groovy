package com.jfrog.bintray.gradle

import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.internal.tasks.SimpleWorkResult
import org.gradle.api.tasks.Copy

import static org.apache.commons.io.FilenameUtils.normalize

class RecordingCopyTask extends Copy {

    def fileUploads = []

    @Override
    protected CopyAction createCopyAction() {
        //Check for non-dir input and make the output path relative to the destination
        def resolver = getFileLookup().getFileResolver(destinationDir)
        return {
                //CopyAction
            CopyActionProcessingStream stream ->
                stream.process {
                        //CopyActionProcessingStreamAction
                    FileCopyDetailsInternal details ->
                        if (!details.isDirectory()) {
                            File target = resolver.resolve(details.getRelativePath().getPathString());
                            def destRelPath = normalize project.relativePath(target)
                            fileUploads << new Artifact(file: target, path: destRelPath)
                            didWork = true
                        }
                }
                new SimpleWorkResult(true)
        }
    }
}