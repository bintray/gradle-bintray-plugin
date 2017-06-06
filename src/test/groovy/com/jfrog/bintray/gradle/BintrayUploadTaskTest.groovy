package com.jfrog.bintray.gradle

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Subject

class BintrayUploadTaskTest extends Specification {

    @Subject task = new ProjectBuilder().build().tasks.create("bintrayUploadTask", BintrayUploadTask)

    def "should throw an exception when user is not configured"() {
        given:
        task.user = null
        task.apiKey = "123"

        when:
        task.bintrayUpload()

        then:
        def ex = thrown(GradleException)
        ex.message == "Failing task 'test:bintrayUpload' because user is not configured"
    }

    def "should throw an exception when api key is not configured"() {
        given:
        task.user = "user"
        task.apiKey = null

        when:
        task.bintrayUpload()

        then:
        def ex = thrown(GradleException)
        ex.message == "Failing task 'test:bintrayUpload' because key is not configured"
    }

    def "should throw an exception when user and api key is not configured"() {
        given:
        task.user = null
        task.apiKey = null

        when:
        task.bintrayUpload()

        then:
        def ex = thrown(GradleException)
        ex.message == "Failing task 'test:bintrayUpload' because user is not configured and key is not configured"
    }
}
