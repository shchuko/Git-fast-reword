package shchuko.git_fast_reword;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class GitFastRewordCli {
    private final String[] args;
    private final Options options = new Options();
    private final HelpFormatter helpFormatter = new HelpFormatter();
    private CommandLine cmd;

    private final Map<String, String> commitsToReword = new HashMap<>();
    private boolean rewordMergeCommits = true;

    private int exitStatus = EXIT_SUCCESS;

    public static void main(String[] args) {
        GitFastRewordCli main = new GitFastRewordCli(args);
        main.run();
        System.exit(main.getExitStatus());
    }

    public GitFastRewordCli(String[] args) {
        this.args = args;
        createParserOptions();
    }

    public void run() {
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            printWrongArgs(e.getMessage());
            return;
        }

        if (loadArgs()) {
            doReword();
        }
    }

    public int getExitStatus() {
        return exitStatus;
    }

    private void createParserOptions() {
        options.addOption(ALLOW_REWORD_MERGES_OPT_SHORT, ALLOW_REWORD_MERGES_OPT_LONG, false, ALLOW_REWORD_MERGES_OPT_INFO);
        options.addOption(HELP_OPT_SHORT, HELP_OPT_LONG, false, HELP_OPT_INFO);
    }

    private boolean loadArgs() {
        if (cmd.hasOption(HELP_OPT_SHORT) || cmd.hasOption(HELP_OPT_LONG)) {
            helpFormatter.printHelp(USAGE, options);
            exitStatus = EXIT_SUCCESS;
            return false;
        }

        rewordMergeCommits = cmd.hasOption(ALLOW_REWORD_MERGES_OPT_SHORT) || cmd.hasOption(ALLOW_REWORD_MERGES_OPT_LONG);
        String[] pureArgs = cmd.getArgs();

        if (pureArgs.length == 1) {
            loadFromFile(pureArgs[0].strip());
            return true;
        }

        if (pureArgs.length >= 2) {
            commitsToReword.put(pureArgs[0].strip(), pureArgs[1].strip().concat(System.lineSeparator()));
            return true;
        }

        printWrongArgs("Not enough args");
        return false;
    }

    private void loadFromFile(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String fileToHashPath;
            while ((fileToHashPath = reader.readLine()) != null) {
                String[] rewordInfo = fileToHashPath.split(",");

                if (rewordInfo.length == 2) {
                    commitsToReword.put(rewordInfo[0].strip(), rewordInfo[1].strip().concat(System.lineSeparator()));
                }
            }
        } catch (IOException e) {
            System.err.println("Input file reading error");
        }
    }

    private void doReword() {
        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(Paths.get(System.getProperty("user.dir")));
            gitFastReword.setAllowRewordMergeCommits(rewordMergeCommits);
            gitFastReword.setInfoPrintStream(System.out);
            gitFastReword.setErrPrintStream(System.err);
            gitFastReword.reword(commitsToReword);

            exitStatus = EXIT_SUCCESS;
        } catch (IOException | RepositoryNotFoundException | RepositoryNotOpenedException | GitOperationFailureException e) {
            System.err.println("An error caused: " + e.getMessage());
            exitStatus = EXIT_FAILURE;
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            exitStatus = EXIT_FAILURE;
        }
    }

    private void printWrongArgs(String msg) {
        this.exitStatus = EXIT_FAILURE;
        System.err.println(msg);
        helpFormatter.printHelp(USAGE, options);
    }

    private static final String HELP_OPT_SHORT = "h";
    private static final String HELP_OPT_LONG = "help";
    private static final String HELP_OPT_INFO = "Print this help";

    private static final String ALLOW_REWORD_MERGES_OPT_SHORT = "m";
    private static final String ALLOW_REWORD_MERGES_OPT_LONG = "reword-merges";
    private static final String ALLOW_REWORD_MERGES_OPT_INFO = "Allow reword merge commits";

    private static final String USAGE = "Git-fast-reword {COMMIT-ID MSG}|{COMMITS-LIST-FILE-PATH} [OPTIONS]";

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;
}
