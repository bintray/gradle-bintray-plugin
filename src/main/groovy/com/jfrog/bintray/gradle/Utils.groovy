package com.jfrog.bintray.gradle

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

class Utils {
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

        DateFormat dateToStringFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
        return isoFormat.format(dateToStringFormat.parse(dateString))
    }
}