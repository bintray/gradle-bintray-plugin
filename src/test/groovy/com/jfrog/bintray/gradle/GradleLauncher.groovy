package com.jfrog.bintray.gradle

/**
 * Created by Eyal BM on 19/11/2014.
 */
class GradleLauncher {
    private def gradleCommandPath
    private def gradleProjectFilePath
    private def cmd

    private List<String> tasks = new ArrayList<String>()
    private Set<String> switches = new HashSet<String>()
    private Map<String, Object> envVars = new HashMap<String, Object>()
    private Map<String, Object> systemProps = new HashMap<String, Object>()

    GradleLauncher(gradleCommandPath, gradleProjectFilePath) {
        this.gradleCommandPath = gradleCommandPath
        this.gradleProjectFilePath = gradleProjectFilePath
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

    private def getCmd() {
        if (cmd == null) {
            createCmd()
        }
        cmd
    }

    def launch() {
        Process p
        try {
            def cmd = getCmd()
            println "Launching Gradle process: $cmd"
            p = Runtime.getRuntime().exec(cmd)

            LogPrinter inputPrinter = new LogPrinter(p.getInputStream())
            LogPrinter errorPrinter = new LogPrinter(p.getErrorStream())

            new Thread(inputPrinter).start()
            new Thread(errorPrinter).start()
            p.waitFor()
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

    private static class LogPrinter implements Runnable {
        private InputStream inputStream

        LogPrinter(InputStream inputStream) {
            this.inputStream = inputStream
        }

        @Override
        public void run() {
            final def processReader =
                    new BufferedReader(new InputStreamReader(inputStream))

            processReader.withReader {
                def line
                while ((line = it.readLine()) != null){
                    println(line)
                }
            }
        }
    }
}
