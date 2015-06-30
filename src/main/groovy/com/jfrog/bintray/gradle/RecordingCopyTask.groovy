package com.jfrog.bintray.gradle

import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.internal.tasks.SimpleWorkResult
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.OutputDirectory

class RecordingCopyTask extends Copy {

    static String NAME = '_bintrayRecordingCopy'

    def fileUploads = []

    @OutputDirectory
    public File getDestinationDir() {
        null
    }

    @Override
    protected CopyAction createCopyAction() {
        def intoDir = getRootSpec().@destinationDir
        if (intoDir instanceof String) {
            throw new InvalidUserDataException("Bintray copy spec 'into' only accepts a relative string path.")
        }
        return {
                //CopyAction
            CopyActionProcessingStream stream ->
                stream.process {
                        //CopyActionProcessingStreamAction
                    FileCopyDetailsInternal details ->
                        if (!details.isDirectory()) {
                            //
                            def destRelPath = intoDir != null ? (intoDir + '/' + details.getPath()) : details.getPath()
                            fileUploads << new Artifact(file: details.file, path: destRelPath)
                            didWork = true
                        }
                }
                new SimpleWorkResult(true)
        }
    }
}