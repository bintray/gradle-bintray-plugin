package com.jfrog.bintray.gradle

import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.project.MavenProject
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

class Utils {
    public static void addHeaders(Map<?,?> headers) {
        headers.put("User-Agent","gradle-bintray-plugin/${new Utils().pluginVersion}")
    }

    public String getPluginVersion() {
        Properties props = new Properties()
        props.load(getClass().classLoader.getResource("bintray.plugin.release.properties").openStream())
        props.get('version')
    }

    /**
     * The method converts a date string in the format of java.util.date toString() into a string in the following format:
     * yyyy-MM-dd'T'HH:mm:ss.SSSZZ
     * In case the input string already has the target format, it is returned as is.
     * If the input string has a different format, a ParseException is thrown.
     */
    public static String toIsoDateFormat(String dateString) throws ParseException {
        if (dateString == null) {
            return null
        }
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
        try {
            isoFormat.parse(dateString)
            return dateString
        } catch (ParseException e) {
        }

        DateFormat dateToStringFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
        return isoFormat.format(dateToStringFormat.parse(dateString))
    }

    public static String readArtifactIdFromPom(File pom) {
        FileReader reader = new FileReader(pom);
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        Model model = mavenreader.read(reader);
        MavenProject project = new MavenProject(model);
        return project.getArtifactId();
    }
}