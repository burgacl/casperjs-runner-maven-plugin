package fr.linsolas.casperjsrunner;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


/**
 * User: Romain Linsolas
 * Date: 09/04/13
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class CasperJSRunnerMojo extends AbstractMojo {

    // Parameters for the plugin

    @Parameter(alias = "casperjs.executable", defaultValue = "casperjs")
    private String casperExec;

    @Parameter(alias = "tests.directory")
    private File testsDir;

    @Parameter(alias = "ignoreTestFailures")
    private boolean ignoreTestFailures = false;

    @Parameter(alias = "verbose")
    private boolean verbose = false;

    // Parameters for the CasperJS options

    @Parameter(alias = "include.javascript")
    private boolean includeJS = true;

    @Parameter(alias = "include.coffeescript")
    private boolean includeCS = true;

    @Parameter(alias = "pre")
    private String pre;

    @Parameter(alias = "post")
    private String post;

    @Parameter(alias = "includes")
    private String includes;

    @Parameter(alias = "xunit")
    private String xUnit;

    @Parameter(alias = "logLevel")
    private String logLevel;

    @Parameter(alias = "direct")
    private boolean direct = false;

    @Parameter(alias = "failFast")
    private boolean failFast = false;

    private int failures = 0;
    private int success = 0;

    private Log log = getLog();

    private void init() throws MojoFailureException {
        if (StringUtils.isBlank(casperExec)) {
            throw new MojoFailureException("CasperJS executable is not defined");
        }
        // Test CasperJS
        int res = executeCommand(casperExec + " --version");
        if (res == -1) {
            // Problem
            throw new MojoFailureException("An error occurred when trying to execute CasperJS");
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        long started = System.currentTimeMillis();
        log.info("Looking for scripts in " + testsDir + "...");
        if (includeJS) {
            executeScripts(".js");
        } else {
            log.info("JavaScript files ignored");
        }
        if (includeCS) {
            executeScripts(".coffee");
        } else {
            log.info("CoffeeScript files ignored");
        }
        log.info("Tests run: " + (failures + success) + ", Success: " + success + " Failures: " + failures +
                ". Time elapsed: " + (System.currentTimeMillis() - started) + "ms.");
        if (!ignoreTestFailures && (failures > 0)) {
            throw new MojoFailureException("There are " + failures + " tests failures");
        }
    }

    private void executeScripts(final String ext) {
        File[] files = testsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return StringUtils.endsWithIgnoreCase(name, ext);
            }
        });
        if (files.length == 0) {
            log.warn("No " + ext + " files found in directory " + testsDir);
        } else {
            for (File f : files) {
                log.debug("Execution of test " + f.getName());
                int res = executeScript(f);
                if (res == 0) {
                    success++;
                } else {
                    log.warn("Test '" + f.getName() + "' has failure");
                    failures++;
                }
            }
        }
    }

    private int executeScript(File f) {
        StringBuffer command = new StringBuffer();
        command.append(casperExec);
        // Option --includes, to includes files before each test execution
        if (StringUtils.isNotBlank(includes)) {
            command.append(" --includes=").append(includes);
        }
        // Option --pre, to execute the scripts before the test suite
        if (StringUtils.isNotBlank(pre)) {
            command.append(" --pre=").append(pre);
        }
        // Option --pre, to execute the scripts after the test suite
        if (StringUtils.isNotBlank(post)) {
            command.append(" --post=").append(post);
        }
        // Option --xunit, to export results in XML file
        if (StringUtils.isNotBlank(xUnit)) {
            command.append(" --xunit=").append(xUnit);
        }
        // Option --fast-fast, to terminate the test suite once a failure is found
        if (failFast) {
            command.append(" --fail-fast");
        }
        // Option --direct, to output log messages to the console
        if (direct) {
            command.append(" --direct");
        }
        command.append(' ').append(f.getAbsolutePath());
        return executeCommand(command.toString());
    }

    private int executeCommand(String command) {
        log.debug("Execute CasperJS command [" + command + "]");
        DefaultExecutor exec = new DefaultExecutor();
        CommandLine line = CommandLine.parse(command);
        try {
            return exec.execute(line);
        } catch (IOException e) {
            if (verbose) {
                log.error("Could not run CasperJS command", e);
            }
            return -1;
        }
    }


}
