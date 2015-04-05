package com.jfrog.bintray.gradle

import com.beust.jcommander.internal.Maps
import com.google.common.collect.Sets

/**
 * Created by user on 19/11/2014.
 */
class GradleLauncher {
    private def gradleCommandPath
    private def gradleProjectFilePath
    private def gradleLogPath
    private def cmd

    private Set<String> tasks = Sets.newHashSet()
    private Set<String> switches = Sets.newHashSet()
    private Map<String, Object> envVars = Maps.newHashMap()
    private Map<String, Object> systemProps = Maps.newHashMap()

    GradleLauncher(gradleCommandPath, gradleProjectFilePath, gradleLogPath) {
        this.gradleCommandPath = gradleCommandPath
        this.gradleProjectFilePath = gradleProjectFilePath
        this.gradleLogPath = gradleLogPath
    }

    GradleLauncher addTask(String gradleTask) {
        tasks.add(gradleTask)
        this
    }

    GradleLauncher addSwitch(String gradleSwitch) {
        switches.add(gradleSwitch)
        this
    }

    GradleLauncher addEnvVar(String name, String value) {
        envVars.put(name, value)
        this
    }

    GradleLauncher addSystemProp(String name, String value) {
        systemProps.put(name, value)
        this
    }

    private def tasksToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(task in tasks) {
            sb.append(task)
            if (c++ < tasks.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }

    private def switchesToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(gradleSwitch in switches) {
            gradleSwitch = gradleSwitch.startsWith("--") ? gradleSwitch : "--${gradleSwitch}"
            sb.append(gradleSwitch)
            if (c++ < switches.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }

    private def envVarsToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(var in envVars) {
            def key = var.key.startsWith("-P") ? var.key : "-P${var.key}"
            sb.append(key).append("=").append(var.value)
            if (c++ < envVars.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }

    private def systemPropsToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(var in systemProps) {
            def key = var.key.startsWith("-D") ? var.key : "-D${var.key}"
            sb.append(key).append("=").append(var.value)
            if (c++ < systemProps.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }

    private void createCmd() {
        cmd = "$gradleCommandPath ${switchesToString()} ${envVarsToString()} ${systemPropsToString()} " +
                "-b $gradleProjectFilePath ${tasksToString()}"
    }

    private def getCmd(File logFile) {
        if (cmd == null) {
            createCmd()
        }
        "$cmd > ${logFile.getCanonicalPath()} 2>&1"
    }

    private File createLogFile(fileName) {
        String path = gradleLogPath
        if (!path.endsWith(File.separator)) {
            path += File.separator
        }
        path += fileName
        File log = new File(path)
        log.createNewFile()
        log
    }

    private def printLog(File logFile) {
        logFile.eachLine { line ->
            println line
        }
        logFile.delete()
    }

    def launch() {
        Process p
        try {
            def logFileName = "gradle.build.${System.currentTimeMillis()}.log"
            File logFile = createLogFile(logFileName)
            def cmd = getCmd(logFile)
            println "Launching Gradle process: $cmd"
            p = Runtime.getRuntime().exec(cmd)
            p.waitFor()
            printLog(logFile)
            println "Gradle process finished with exit code ${p.exitValue()}"
            p.exitValue()
        } finally {
            if (p != null) {
                if (p.getInputStream() != null) {
                    p.getInputStream().close()
                }
                if (p.getOutputStream() != null) {
                    p.getOutputStream().close()
                }
                if (p.getErrorStream() != null) {
                    p.getErrorStream().close()
                }
            }
        }
    }
}
