package shchuko.git_fast_reword;

import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;

/**
 * A utility to do fast git commit messages' reword
 *
 * @author Vladislav Yaroahshchuk (yaroshchuk2000@gmail.com)
 */
public class GitFastReword implements AutoCloseable {
    private Repository repository;

    private boolean allowRewordMergeCommits;
    private String userName;
    private String userEmail;

    private String currentBranchFullName;
    private ObjectId commitRebaseOntoId;
    private int commitRebaseOntoCommitTime;

    // <commit id, new commit message>
    private final Map<ObjectId, String> commitsToReword = new HashMap<>();

    // <old commit id, commit id after visit>
    private final Map<ObjectId, ObjectId> visitedCommits = new HashMap<>();

    private PrintStream infoPrintStream;
    private PrintStream errPrintStream;


    /**
     * Create git fast reword utility instance
     */
    public GitFastReword() {
        reset();
    }

    /**
     * Set path to existing repository to operate with
     *
     * @param repoPath Path to repo to operate with (.git's parent directory)
     * @throws RepositoryNotFoundException If given path does not exist or a .git directory does not exist
     * @throws IOException                 In case of other I/O operations errors
     */
    public void openRepository(Path repoPath) throws RepositoryNotFoundException, IOException {
        close();

        if (!repoPath.toFile().isDirectory()) {
            throw new IOException("Directory not exists " + repoPath.toAbsolutePath().toString());
        }

        try {
            repository = new FileRepositoryBuilder()
                    .addCeilingDirectory(repoPath.toFile())
                    .findGitDir(repoPath.toFile())
                    .build();

        } catch (Exception e) {
            throw new RepositoryNotFoundException("Git repository not found " + repoPath.toAbsolutePath().toString(), e);
        }
    }

    /**
     * Check is repository opened (by {@link #openRepository(Path)})
     *
     * @return True if repository opened, false if not
     */
    public boolean isOpen() {
        return repository != null;
    }

    /**
     * Is rewording merge commits allowed
     *
     * @return True if rewording merge commits allowed, otherwise false
     */
    public boolean isAllowedRewordMergeCommits() {
        return allowRewordMergeCommits;
    }

    /**
     * Set allow/forbid rewording merge commits
     *
     * @param allowRewordMergeCommits Pass true to allow rewording merge commits, false to deny
     */
    public void setAllowRewordMergeCommits(boolean allowRewordMergeCommits) {
        this.allowRewordMergeCommits = allowRewordMergeCommits;
    }

    /**
     * Reword a commit message by its revision string. Commit should be reachable from current branch head
     *
     * @param commitRevStr A string to identify the commit (sha-1 hash, HEAD^2, ...)
     * @param newMessage   New commit message
     * @throws RepositoryNotOpenedException If the repository is not opened (by {@link #openRepository(Path)})
     * @throws GitOperationFailureException In case of any operations errors (ex. commit not exists)
     */
    public void reword(String commitRevStr, String newMessage) throws RepositoryNotOpenedException, GitOperationFailureException {
        Map<String, String> map = new HashMap<>();
        map.put(commitRevStr, newMessage);
        reword(map);
    }

    /**
     * Reword commits messages by revision strings. Commits should be reachable from current branch head and have common ancestor
     *
     * @param commitsData Key - string to identify the commit (sha-1 hash, HEAD^2, ...), value - new commit message
     * @throws RepositoryNotOpenedException If the repository is not opened (by {@link #openRepository(Path)})
     * @throws GitOperationFailureException In case of any operations errors (ex. rebase not finished/commit not found)
     */
    public void reword(Map<String, String> commitsData) throws RepositoryNotOpenedException, GitOperationFailureException {
        try {
            if (!isOpen()) {
                throw new RepositoryNotOpenedException();
            }

            if (!isRepositoryStateSafe()) {
                throw new GitOperationFailureException("Repository is in an unsafe state");
            }

            tryLoadUserConfig();
            if (userName == null || userEmail == null) {
                throw new GitOperationFailureException("Missing user.name or user.email");
            }

            try {
                saveCurrentBranch();
            } catch (IOException e) {
                throw new GitOperationFailureException("Error while determining current branch", e);
            }

            try {
                if (!isHeadNormal()) {
                    throw new GitOperationFailureException("HEAD is detached or not exists");
                }
            } catch (IOException e) {
                throw new GitOperationFailureException("Error while determining current HEAD ref", e);
            }

            try {
                loadCommitsToReword(commitsData);
            } catch (IOException e) {
                throw new GitOperationFailureException("Error while loading repository commits", e);
            }

            if (commitsToReword.isEmpty()) {
                printInfoMsg("Nothing to reword", LogConstants.INFO);
            } else {
                commitRebaseOntoId = null;
                try {
                    findCommitRebaseOnto();
                } catch (IOException e) {
                    commitRebaseOntoId = null;
                }

                if (commitRebaseOntoId == null) {
                    throw new GitOperationFailureException("Can't found common ancestor for given commits");
                }

                try {
                    doReword();
                } catch (IOException e) {
                    String restoreStatus = tryRestoreHeadRef() ? "succeed" : "failed";
                    throw new GitOperationFailureException("Fatal error, restore HEAD " + restoreStatus, e);
                }

                try {
                    linkBranchesToNewCommits();
                } catch (IOException e) {
                    throw new GitOperationFailureException("Fatal error, please restore git refs manually", e);
                }
            }
        } finally {
            reset();
        }
    }

    /**
     * Implementation of AutoClosable interface
     */
    @Override
    public void close() {
        if (repository != null) {
            repository.close();
            repository = null;
        }
        userName = null;
        userEmail = null;
        reset();
    }

    /**
     * Set info messages print stream
     *
     * @param infoPrintStream Print stream for info messages. To disable info messages pass null
     */
    public void setInfoPrintStream(PrintStream infoPrintStream) {
        this.infoPrintStream = infoPrintStream;
    }

    /**
     * Set error messages print stream
     *
     * @param errPrintStream Print stream for error messages. To disable error messages pass null
     */
    public void setErrPrintStream(PrintStream errPrintStream) {
        this.errPrintStream = errPrintStream;
    }

    /**
     * Get current info messages print stream
     *
     * @return Current info messages print stream
     */
    public PrintStream getInfoPrintStream() {
        return infoPrintStream;
    }

    /**
     * Get current error messages print stream
     *
     * @return Current error messages print stream
     */
    public PrintStream getErrPrintStream() {
        return errPrintStream;
    }

    /**
     * Reset {@link GitFastReword} instance after a reword, not closes repository
     */
    private void reset() {
        visitedCommits.clear();
        commitsToReword.clear();

        currentBranchFullName = null;
        commitRebaseOntoId = null;
        commitRebaseOntoCommitTime = Integer.MAX_VALUE;
    }

    /**
     * Checks is repository ready to reword
     *
     * @return true if ready, false if not
     */
    private boolean isRepositoryStateSafe() {
        return repository.getRepositoryState().equals(RepositoryState.SAFE) && repository.getRepositoryState().canCheckout();
    }

    /**
     * Load user name and user email
     */
    private void tryLoadUserConfig() {
        Config config = repository.getConfig();
        userName = config.getString("user", null, "name");
        userEmail = config.getString("user", null, "email");
    }

    /**
     * Save current branch6:29:48 PM: Task execution finished 'run'.
     *
     * @throws IOException In case of any git filesystem problems
     */
    private void saveCurrentBranch() throws IOException {
        currentBranchFullName = repository.getFullBranch();
    }

    /**
     * Check is head normal
     *
     * @return true if is normal, false if not exists or detached
     * @throws IOException In case of any fatal JGit errors
     */
    boolean isHeadNormal() throws IOException {
        return repository.resolve(Constants.HEAD) != null && repository.getRefDatabase().exactRef(Constants.HEAD).isSymbolic();
    }

    /**
     * Reword commits messages by revision strings. Commits should be reachable from current branch head and have common ancestor
     *
     * @param commitsData Key - string to identify the commit (sha-1 hash, HEAD^2, ...), value - new commit message
     * @throws IOException In case of any fatal JGit errors
     */
    private void loadCommitsToReword(Map<String, String> commitsData) throws IOException {
        commitsToReword.clear();

        // <commit id, new commit message>
        Map<ObjectId, String> existCommits = new HashMap<>();

        // Filtering commits exist in this repository
        for (var item : commitsData.entrySet()) {
            if (item.getKey() == null || item.getValue() == null) {
                printErrMsg("Commit with null field(s)", LogConstants.SKIP);
                continue;
            }

            if (item.getValue().isEmpty()) {
                printErrMsg(item.getKey() + " has empty message", LogConstants.WARN);
            }

            try {
                ObjectId objectId = repository.resolve(item.getKey());

                if (objectId == null || repository.open(objectId).getType() != Constants.OBJ_COMMIT) {
                    printErrMsg(item.getKey() + " is not a commit", LogConstants.SKIP);
                } else {
                    existCommits.put(objectId, item.getValue());
                }
            } catch (AmbiguousObjectException e) {
                printErrMsg(item.getKey() + " more than one object which matches", LogConstants.SKIP);
            } catch (IOException | RevisionSyntaxException e) {
                printErrMsg(item.getKey() + " not found", LogConstants.SKIP);
            }
        }

        // Filtering commits reachable current branch head
        RevWalk walk = new RevWalk(repository);
        walk.markStart(walk.parseCommit(repository.resolve(currentBranchFullName)));

        Iterator<RevCommit> iterator = walk.iterator();
        while (iterator.hasNext() && !existCommits.isEmpty()) {
            RevCommit commit = iterator.next();
            ObjectId commitId = commit.getId();

            if (existCommits.containsKey(commitId)) {
                String newCommitMsg = existCommits.get(commit.getId());
                existCommits.remove(commitId);

                if (commit.getParentCount() == 0) {
                    printErrMsg(commit.getName() + " has no parents, cannot be reworded", LogConstants.SKIP);
                } else if (commit.getParentCount() == 1 || allowRewordMergeCommits) {
                    commitsToReword.put(commit.getId(), newCommitMsg);
                } else if (commit.getParentCount() >= 2) {
                    printErrMsg(commit.getName() + " is merge commit", LogConstants.SKIP);
                }
            }
        }
        walk.dispose();

        for (var commit : existCommits.entrySet()) {
            if (!commitsToReword.containsKey(commit.getKey())) {
                printErrMsg(commit.getKey().getName() + " exists, but not found on current branch", LogConstants.SKIP);
            }
        }

    }

    /**
     * Find common ancestor fot all passed to reword commits
     *
     * @throws IOException In case of any fatal JGit errors
     */
    private void findCommitRebaseOnto() throws IOException {
        RevWalk walk = new RevWalk(repository);
        walk.setRevFilter(RevFilter.MERGE_BASE);
        for (var commitId : commitsToReword.keySet()) {
            walk.markStart(walk.parseCommit(commitId));
        }

        RevCommit commonAncestorCommit = walk.next();
        // Used 1st parent of common ancestor commit if exists
        commitRebaseOntoId = commonAncestorCommit != null ? commonAncestorCommit.getParent(0) : null;

        if (commitRebaseOntoId != null) {
            commitRebaseOntoCommitTime = walk.parseCommit(commitRebaseOntoId).getCommitTime();
        } else {
            commitRebaseOntoCommitTime = Integer.MAX_VALUE;
        }

    }

    /**
     * Reword commits
     *
     * @throws IOException In case of any fatal JGit errors
     */
    private void doReword() throws IOException {
        visitedCommits.clear();

        visitedCommits.put(commitRebaseOntoId, commitRebaseOntoId);
        String refLogMsg = RefLogConstants.REBASE_START + commitRebaseOntoId.getName();
        updateRef(Constants.HEAD, commitRebaseOntoId, true, refLogMsg);
        printInfoMsg(refLogMsg, LogConstants.INFO);

        RevWalk walk = new RevWalk(repository);
        ObjectInserter objectInserter = repository.newObjectInserter();
        dfsReword(walk, objectInserter, repository.resolve(currentBranchFullName));
        walk.dispose();
    }

    /**
     * Part of reword algorithm uses depth-first search
     *
     * @param walk           A RevWalk to be used to parse commits
     * @param objectInserter In ObjectInserter to be used to create commits in repository
     * @param oldCommitId    Id of the commit to to be copied
     * @return Id of the passed commit if no changes in ancestors, otherwise id of edited commit copy
     * @throws IOException In case of any fatal JGit errors
     */
    private ObjectId dfsReword(RevWalk walk, ObjectInserter objectInserter, ObjectId oldCommitId) throws IOException {
        if (oldCommitId.equals(commitRebaseOntoId)) {
            return oldCommitId;
        }

        if (visitedCommits.containsKey(oldCommitId)) {
            ObjectId newCommitId = visitedCommits.get(oldCommitId);

            String refLogMsg = RefLogConstants.REBASE_RESET + "'" + newCommitId.getName() + "'";
            updateRef(Constants.HEAD, newCommitId, true, refLogMsg);
            printInfoMsg(refLogMsg, LogConstants.INFO);

            return newCommitId;
        }

        RevCommit oldCommit = walk.parseCommit(oldCommitId);
        if (oldCommit.getCommitTime() < commitRebaseOntoCommitTime) {
            String refLogMsg = RefLogConstants.REBASE_RESET + "'" + oldCommitId.getName() + "'";
            updateRef(Constants.HEAD, oldCommitId, true, refLogMsg);
            printInfoMsg(refLogMsg, LogConstants.INFO);
            visitedCommits.put(oldCommitId, oldCommitId);
            return oldCommitId;
        }

        List<ObjectId> parentsIds = new ArrayList<>();
        boolean newParentCreated = false;
        for (var parent : oldCommit.getParents()) {
            ObjectId newParentId = dfsReword(walk, objectInserter, parent.getId());
            parentsIds.add(newParentId);

            if (newParentId != parent.getId()) {
                newParentCreated = true;
            }
        }

        String newCommitMessage = commitsToReword.get(oldCommit.getId());
        if (!newParentCreated && newCommitMessage == null) {
            updateRef(Constants.HEAD, oldCommitId, true, RefLogConstants.REBASE_FAST_FORWARD);
            printInfoMsg(RefLogConstants.REBASE_FAST_FORWARD, LogConstants.INFO);
            return oldCommitId;
        }

        CommitBuilder builder = new CommitBuilder();
        builder.setTreeId(oldCommit.getTree().getId());
        builder.setParentIds(parentsIds);
        builder.setAuthor(oldCommit.getAuthorIdent());
        builder.setCommitter(new PersonIdent(userName, userEmail));
        builder.setEncoding(oldCommit.getEncoding());
        builder.setMessage(newCommitMessage != null ? newCommitMessage : oldCommit.getFullMessage());

        ObjectId newCommitId = objectInserter.insert(builder);
        visitedCommits.put(oldCommitId, newCommitId);
        RevCommit newCommit = walk.parseCommit(newCommitId);

        String refLogMsg;
        if (newCommitMessage != null) {
            refLogMsg = RefLogConstants.REBASE_REWORD + newCommit.getShortMessage();
        } else {
            refLogMsg = RefLogConstants.REBASE_PICK + newCommit.getShortMessage();
        }
        updateRef(Constants.HEAD, newCommitId, true, refLogMsg);
        printInfoMsg(refLogMsg, LogConstants.INFO);

        return newCommitId;
    }

    /**
     * Try restore HEAD onto current branch head
     */
    private boolean tryRestoreHeadRef() {
        try {
            updateRef(Constants.HEAD, currentBranchFullName, false, RefLogConstants.RESET + currentBranchFullName);
        } catch (IOException e) {
            printErrMsg("Repository recover unsuccessful", LogConstants.ERR);
            return false;
        }
        return true;
    }

    /**
     * Link branches to new commits
     *
     * @throws IOException In case of any fatal JGit errors
     */
    private void linkBranchesToNewCommits() throws IOException {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);

        String branchRefLogMsg = RefLogConstants.REBASE_FINISH + currentBranchFullName + " onto " + commitRebaseOntoId.getName();
        updateRef(currentBranchFullName, lastCommitId, false, branchRefLogMsg);
        printInfoMsg(branchRefLogMsg, LogConstants.INFO);

        String headRefLogMsg = RefLogConstants.REBASE_FINISH + "returning to " + currentBranchFullName;
        updateRef(Constants.HEAD, currentBranchFullName, false, headRefLogMsg);
        printInfoMsg(headRefLogMsg, LogConstants.INFO);

    }

    void updateRef(String revStr, ObjectId targetCommitId, boolean detach, String refLogMsg) throws IOException {
        RefUpdate headUpdate = repository.getRefDatabase().newUpdate(revStr, detach);
        headUpdate.setRefLogIdent(new PersonIdent(userName, userEmail));
        headUpdate.setRefLogMessage(refLogMsg, false);
        headUpdate.setNewObjectId(targetCommitId);
        headUpdate.setForceUpdate(true);
        headUpdate.update();
    }

    void updateRef(String revStr, String targetRevStr, boolean detach, String refLogMsg) throws IOException {
        RefUpdate headUpdate = repository.getRefDatabase().newUpdate(revStr, detach);
        headUpdate.setRefLogIdent(new PersonIdent(userName, userEmail));
        headUpdate.setRefLogMessage(refLogMsg, false);
        headUpdate.setForceUpdate(true);
        headUpdate.link(targetRevStr);
    }

    /**
     * Print a message to errPrintStream
     *
     * @param msg       Message to print
     * @param beforeMsg Message prefix
     */
    private void printErrMsg(String msg, String beforeMsg) {
        if (errPrintStream != null) {
            if (beforeMsg == null) {
                errPrintStream.println(msg);
            } else {
                errPrintStream.println(beforeMsg + " " + msg);
            }
        }
    }

    /**
     * Print a message to infoPrintStream
     *
     * @param msg       Message to print
     * @param beforeMsg Message prefix
     */
    private void printInfoMsg(String msg, String beforeMsg) {
        if (infoPrintStream != null) {
            if (beforeMsg == null) {
                infoPrintStream.println(msg);
            } else {
                infoPrintStream.println(beforeMsg + " " + msg);
            }
        }
    }

    private static class LogConstants {
        static final String SKIP = "[ Skip ]";
        static final String ERR = "[ Err  ]";
        static final String WARN = "[ Warn ]";
        static final String INFO = "[ Info ]";
    }

    private static class RefLogConstants {
        static final String RESET = "reset: moving to ";
        static final String REBASE_START = "rebase (start): checkout ";
        static final String REBASE_FAST_FORWARD = "rebase: fast-forward";
        static final String REBASE_PICK = "rebase (pick): ";
        static final String REBASE_REWORD = "rebase (reword): ";
        static final String REBASE_RESET = "rebase (reset): ";
        static final String REBASE_FINISH = "rebase (finish): ";
    }
}
