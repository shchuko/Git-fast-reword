package shchuko.git_fast_reword;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Vladislav Yaroahshchuk (yaroshchuk2000@gmail.com)
 */
public class GitFastRewordUnitTests {
    private static final String MY_EXISTING_REPO_URI = "https://github.com/shchuko/ScratchedHologramFrom3D.git";

    private static final String NOT_EXISTING_DIR_NAME = "notExistingDirPrivet";
    private File tempRepoDir;

    @Rule
    public TemporaryFolder tempRoot = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        tempRepoDir = tempRoot.newFolder();
    }

    @Test(expected = IOException.class)
    public void openThrowsIOExceptionTest() throws IOException, RepositoryNotFoundException {
        Path notExists = tempRepoDir.toPath().resolve(NOT_EXISTING_DIR_NAME);
        Assert.assertFalse("Test dir exists but should not " + notExists.toString(), notExists.toFile().isDirectory());

        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.openRepository(notExists);
        gitFastReword.close();
    }

    @Test(expected = RepositoryNotFoundException.class)
    public void openNotAGitRepositoryThrowTest() throws RepositoryNotFoundException, IOException {
        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.openRepository(tempRepoDir.toPath());
        gitFastReword.close();
    }

    @Test
    public void isRepositoryOpenedTest() throws IOException, RepositoryNotFoundException {
        Path blankRepoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.EMPTY, tempRepoDir);
        Assert.assertNotNull("Blank repository creation unsuccessful", blankRepoPath);

        GitFastReword gitFastReword = new GitFastReword();
        Assert.assertFalse(gitFastReword.isOpen());

        gitFastReword.openRepository(blankRepoPath);
        Assert.assertTrue(gitFastReword.isOpen());

        gitFastReword.close();
        Assert.assertFalse(gitFastReword.isOpen());
    }

    @Test
    public void isAllowedRewordMergeCommitsTest() {
        GitFastReword gitFastReword = new GitFastReword();
        Assert.assertFalse(gitFastReword.isAllowedRewordMergeCommits());

        gitFastReword.setAllowRewordMergeCommits(true);
        Assert.assertTrue(gitFastReword.isAllowedRewordMergeCommits());

        gitFastReword.setAllowRewordMergeCommits(false);
        Assert.assertFalse(gitFastReword.isAllowedRewordMergeCommits());
        gitFastReword.close();
    }

    @Test
    public void setInfoPrintStreamTest() {
        GitFastReword gitFastReword = new GitFastReword();
        Assert.assertNull(gitFastReword.getInfoPrintStream());

        PrintStream expected = System.out;
        gitFastReword.setInfoPrintStream(expected);
        Assert.assertEquals(expected, gitFastReword.getInfoPrintStream());
        gitFastReword.close();
    }

    @Test
    public void setErrPrintStreamTest() {
        GitFastReword gitFastReword = new GitFastReword();
        Assert.assertNull(gitFastReword.getErrPrintStream());

        PrintStream expected = System.out;
        gitFastReword.setErrPrintStream(expected);
        Assert.assertEquals(expected, gitFastReword.getErrPrintStream());
        gitFastReword.close();
    }

    @Test(expected = RepositoryNotOpenedException.class)
    public void rewordRepositoryNotOpenedThrowTest() throws RepositoryNotOpenedException, GitOperationFailureException {
        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.reword("HEAD", "SomeMessage");
        gitFastReword.close();
    }

    @Test(expected = GitOperationFailureException.class)
    public void rewordNoHeadRefTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.EMPTY, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.openRepository(repoPath);

        gitFastReword.reword("HEAD", "SomeHeadMsg");
        gitFastReword.close();
    }

    @Test(expected = GitOperationFailureException.class)
    public void rewordHeadDetachedTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.HEAD_DETACHED, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.openRepository(repoPath);

        gitFastReword.reword("HEAD", "SomeHeadMsg");
        gitFastReword.close();
    }


    @Test(expected = GitOperationFailureException.class)
    public void rewordSimulateFatalErrorTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.openRepository(repoPath);

        tempRoot.delete();

        gitFastReword.reword("HEAD", "SomeHeadMsg");
        gitFastReword.close();
    }

    @Test(expected = GitOperationFailureException.class)
    public void rewordSimulateUnresolvedMergeConflict()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.MERGE_CONFLICT, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        GitFastReword gitFastReword = new GitFastReword();
        gitFastReword.openRepository(repoPath);


        gitFastReword.reword("HEAD", "SomeHeadMsg");
        gitFastReword.close();

        tempRoot.delete();
    }

    @Test
    public void rewordNotExistingCommit()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        List<String> messagesBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesBeforeReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);

            gitFastReword.reword("NotACommit", "Some message");
        }

        List<String> messagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesAfterReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
        }

        Assert.assertEquals(messagesBeforeReword, messagesAfterReword);
    }

    @Test
    public void rewordRewordLastTwoCommitsTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//          On branch 'master'
//
//          * (HEAD -> master) Commit 5 ->[reword]->"New last commit message"
//          * Commit 4                  ->[reword]->"New prev last commit message"
//          * Commit 3
//          * Commit 2
//          * Commit 1


        String lastCommitMsg = "New last commit message";
        String prevLastCommitMsg = "New prev last commit message";

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);


        List<String> messagesBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesBeforeReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);

            gitFastReword.reword("HEAD", lastCommitMsg);
            gitFastReword.reword("HEAD~1", prevLastCommitMsg);
        }

        List<String> messagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesAfterReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
        }

        List<String> expectedMessages = new ArrayList<>(messagesBeforeReword);

        expectedMessages.set(0, lastCommitMsg);
        expectedMessages.set(1, prevLastCommitMsg);

        Assert.assertEquals(expectedMessages, messagesAfterReword);
    }

    @Test
    public void rewordRewordMiddleCommitTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) Commit 5
//        * Commit 4
//        * Commit 3                  ->[reword]->"HEAD~2 commit message"
//        * Commit 2
//        * Commit 1

        final int rewordCommitFromHeadNo = 2;
        final String commitMsg = "HEAD~" + rewordCommitFromHeadNo + " commit message";

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        List<String> messagesBeforeReword = new ArrayList<>();
        List<RevTree> treesBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            Iterable<RevCommit> log = git.log().call();
            log.forEach(commit -> messagesBeforeReword.add(commit.getFullMessage()));
            log.forEach(commit -> treesBeforeReword.add(commit.getTree()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword("HEAD~" + rewordCommitFromHeadNo, commitMsg);
        }

        List<String> messagesAfterReword = new ArrayList<>();
        List<RevTree> treesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            Iterable<RevCommit> log = git.log().call();
            log.forEach(commit -> messagesAfterReword.add(commit.getFullMessage()));
            log.forEach(commit -> treesAfterReword.add(commit.getTree()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
            return;
        }

        Assert.assertEquals(treesBeforeReword, treesAfterReword);

        List<String> expectedMessages = new ArrayList<>(messagesBeforeReword);
        expectedMessages.set(rewordCommitFromHeadNo, commitMsg);
        Assert.assertEquals(expectedMessages, messagesAfterReword);
    }

    @Test
    public void rewordNotDestroysOldCommitsTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) Commit 5
//        * Commit 4
//        * Commit 3 ->[reword]->"HEAD~2 commit message"
//        * Commit 2
//        * Commit 1

        final int rewordCommitFromHeadNo = 2;
        final String commitMsg = "HEAD~" + rewordCommitFromHeadNo + " commit message";

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        ObjectId oldHeadId;
        List<RevCommit> logBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(logBeforeReword::add);

            Repository repository = git.getRepository();
            oldHeadId = repository.resolve("HEAD");
            repository.close();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword("HEAD~" + rewordCommitFromHeadNo, commitMsg);
        }

        List<RevCommit> logAfterRewordAndReset = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setName(oldHeadId.getName()).call();
            git.log().call().forEach(logAfterRewordAndReset::add);
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
        }

        Assert.assertEquals(logBeforeReword.size(), logAfterRewordAndReset.size());
        for (int i = 0; i < logBeforeReword.size(); ++i) {
            RevCommit commitExpected = logBeforeReword.get(i);
            RevCommit commitActual = logAfterRewordAndReset.get(i);

            Assert.assertEquals(commitExpected.getFullMessage(), commitActual.getFullMessage());
            Assert.assertEquals(commitExpected.getAuthorIdent(), commitActual.getAuthorIdent());
            Assert.assertEquals(commitExpected.getCommitterIdent(), commitActual.getCommitterIdent());
            Assert.assertArrayEquals(commitExpected.getParents(), commitActual.getParents());
            Assert.assertEquals(commitExpected.getTree(), commitActual.getTree());
            Assert.assertEquals(commitExpected.getEncoding(), commitActual.getEncoding());
            Assert.assertEquals(commitExpected.getId(), commitActual.getId());
        }
    }

    @Test
    public void rewordNotRewordsCommitWithoutParentsTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) Commit 5
//        * Commit 4
//        * Commit 3
//        * Commit 2
//        * Commit 1      ->[won't be changed]

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        List<RevCommit> logBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(logBeforeReword::add);
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        final int commitIndex = 4;
        final String commitMsg = "HEAD~" + commitIndex + " commit message";

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword("HEAD~" + commitIndex, commitMsg);
        }

        List<RevCommit> logAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(logAfterReword::add);
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
        }

        Assert.assertEquals(logBeforeReword.size(), logAfterReword.size());
        for (int i = 0; i < logBeforeReword.size(); ++i) {
            RevCommit commitExpected = logBeforeReword.get(i);
            RevCommit commitActual = logAfterReword.get(i);

            Assert.assertEquals(commitExpected.getFullMessage(), commitActual.getFullMessage());
            Assert.assertEquals(commitExpected.getAuthorIdent(), commitActual.getAuthorIdent());
            Assert.assertEquals(commitExpected.getCommitterIdent(), commitActual.getCommitterIdent());
            Assert.assertArrayEquals(commitExpected.getParents(), commitActual.getParents());
            Assert.assertEquals(commitExpected.getTree(), commitActual.getTree());
            Assert.assertEquals(commitExpected.getEncoding(), commitActual.getEncoding());
            Assert.assertEquals(commitExpected.getId(), commitActual.getId());
        }
    }

    @Test
    public void rewordSkipRewordMergeCommitsTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) Merge branch 'b1'    ->[won't be changed]
//        |\
//        | * (b1) 2nd on b1
//        | * 1st on b1
//        * | 4th on master
//        | | * (b2) 2nd on b2
//        | | * 1st on b2
//        | |/
//        |/|
//        * | 3rd on master
//        |/
//        * 2nd on master
//        * 1st on master

        String rewordCommitIdent = "HEAD";
        String rewordCommitMsg = "SomeMessage";

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.MERGED_BRANCHES, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        List<String> messagesBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesBeforeReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.setAllowRewordMergeCommits(false);
            gitFastReword.reword(rewordCommitIdent, rewordCommitMsg);
        }

        List<String> messagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesAfterReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        Assert.assertEquals(messagesBeforeReword, messagesAfterReword);
    }

    @Test
    public void rewordDoRewordMergeCommitsTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) Merge branch 'b1' ->[reword]->"Merge branch 'b1' reword"
//        |\
//        | * (b1) 2nd on b1
//        | * 1st on b1
//        * | 4th on master
//        | | * (b2) 2nd on b2
//        | | * 1st on b2
//        | |/
//        |/|
//        * | 3rd on master
//        |/
//        * 2nd on master
//        * 1st on master
        String rewordCommitIdent = "HEAD";
        String rewordCommitMsg = "Merge branch 'b1' reword";

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.MERGED_BRANCHES, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        List<String> messagesBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesBeforeReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.setAllowRewordMergeCommits(true);
            gitFastReword.reword(rewordCommitIdent, rewordCommitMsg);
        }

        List<String> messagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.log().call().forEach(commit -> messagesAfterReword.add(commit.getFullMessage()));
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        List<String> expected = new ArrayList<>(messagesBeforeReword);
        expected.set(0, rewordCommitMsg);
        Assert.assertEquals(expected, messagesAfterReword);
    }

    @Test
    public void rewordSkipRewordCommitFromOtherBranchTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) 4th on master
//        | * (b2) 2nd on b2  ->[won't be changed]
//        | * 1st on b2
//        |/
//        * 3rd on master     ->[reword]->"3rd on master reworded"
//        | * (b1) 2nd on b1  ->[won't be changed]
//        | * 1st on b1
//        |/
//        * 2nd on master
//        * 1st on master

        String commitToRewordIdent = "HEAD~1";
        int commitToRewordIndex = 1;
        String commitToRewordMsg = "3rd on master reworded";

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.NOT_MERGED_BRANCHES, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        List<String> masterMessagesBeforeReword = new ArrayList<>();
        List<String> b1MessagesBeforeReword = new ArrayList<>();
        List<String> b2MessagesBeforeReword = new ArrayList<>();

        String b1HeadHash;
        String b2HeadHash;

        try (Git git = Git.open(repoPath.toFile())) {

            git.log().call().forEach(commit -> masterMessagesBeforeReword.add(commit.getFullMessage()));

            git.checkout().setName("b1").call();
            git.log().call().forEach(commit -> b1MessagesBeforeReword.add(commit.getFullMessage()));
            b1HeadHash = git.getRepository().resolve(Constants.HEAD).getName();

            git.checkout().setName("b2").call();
            git.log().call().forEach(commit -> b2MessagesBeforeReword.add(commit.getFullMessage()));
            b2HeadHash = git.getRepository().resolve(Constants.HEAD).getName();

            git.checkout().setName("master").call();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }


        Map<String, String> commitsToReword = new HashMap<>();
        commitsToReword.put(b1HeadHash, "won't be written b1 head message");
        commitsToReword.put(b2HeadHash, "won't be written b2 head message");
        commitsToReword.put(commitToRewordIdent, commitToRewordMsg);


        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream errPrintStream = new PrintStream(byteArrayOutputStream, true);
        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.setErrPrintStream(errPrintStream);
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword(commitsToReword);
        }

        List<String> masterMessagesAfterReword = new ArrayList<>();
        List<String> b1MessagesAfterReword = new ArrayList<>();
        List<String> b2MessagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {

            git.log().call().forEach(commit -> masterMessagesAfterReword.add(commit.getFullMessage()));

            git.checkout().setName("b1").call();
            git.log().call().forEach(commit -> b1MessagesAfterReword.add(commit.getFullMessage()));

            git.checkout().setName("b2").call();
            git.log().call().forEach(commit -> b2MessagesAfterReword.add(commit.getFullMessage()));

            git.checkout().setName("master").call();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
            return;
        }

        List<String> masterMessagesExpected = new ArrayList<>(masterMessagesBeforeReword);
        masterMessagesExpected.set(commitToRewordIndex, commitToRewordMsg);
        Assert.assertEquals(masterMessagesExpected, masterMessagesAfterReword);
        Assert.assertEquals(b1MessagesBeforeReword, b1MessagesAfterReword);
        Assert.assertEquals(b2MessagesBeforeReword, b2MessagesAfterReword);
        Assert.assertFalse(byteArrayOutputStream.toString().trim().isEmpty());
    }

    @Test
    public void rewordRebaseMergesTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) Merge branch 'b1'
//        |\
//        | * (b1) 2nd on b1  ->[reword]->"2nd on b1 reword"
//        | * 1st on b1
//        * | 4th on master
//        | | * (b2) 2nd on b2
//        | | * 1st on b2
//        | |/
//        |/|
//        * | 3rd on master   ->[reword]->"3rd on master reword" (HEAD~2)
//        |/
//        * 2nd on master     ->[reword]->"2nd on master reword" (HEAD~3)
//        * 1st on master

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.MERGED_BRANCHES, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        Map<String, String> commitsToReword = new HashMap<>();
        commitsToReword.put("HEAD^2", "2nd on b1 reword");
        commitsToReword.put("HEAD~2", "3rd on master reword");
        commitsToReword.put("HEAD~3", "2nd on master reword");

        List<String> b1MessagesBeforeReword = new ArrayList<>();
        List<String> b2MessagesBeforeReword = new ArrayList<>();

        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setName("b1").call();
            git.log().call().forEach(commit -> b1MessagesBeforeReword.add(commit.getFullMessage()));

            git.checkout().setName("b2").call();
            git.log().call().forEach(commit -> b2MessagesBeforeReword.add(commit.getFullMessage()));

            git.checkout().setName("master").call();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword(commitsToReword);
        }

        List<String> b1MessagesAfterReword = new ArrayList<>();
        List<String> b2MessagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setName("b1").call();
            git.log().call().forEach(commit -> b1MessagesAfterReword.add(commit.getFullMessage()));

            git.checkout().setName("b2").call();
            git.log().call().forEach(commit -> b2MessagesAfterReword.add(commit.getFullMessage()));

            git.checkout().setName("master").call();

            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository)) {
                for (Map.Entry<String, String> mapEntry : commitsToReword.entrySet()) {
                    String expectedMsg = mapEntry.getValue();
                    String actualMsg = revWalk.parseCommit(repository.resolve(mapEntry.getKey())).getFullMessage();
                    Assert.assertEquals(expectedMsg, actualMsg);
                }
            }

            repository.close();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
            return;
        }

        Assert.assertEquals(b1MessagesBeforeReword, b1MessagesAfterReword);
        Assert.assertEquals(b2MessagesBeforeReword, b2MessagesAfterReword);
    }

    @Test(expected = GitOperationFailureException.class)
    public void rewordNoCommonCommitsAncestorTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) 3rd on master
//        *   Merge branch 'orphan_b'
//        |\
//        | * (orphan_b) 2nd on orphan_b    ->[reword]->[no common ancestor]
//        | * 1st on orphan_b
//        * 3rd on master                   ->[reword]->[no common ancestor]
//        * 2nd on master
//        * 1st on master
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.MERGED_ORPHAN, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        Map<String, String> commitsToReword = new HashMap<>();
        commitsToReword.put("HEAD~1^2", "2nd on orphan_b reword");
        commitsToReword.put("HEAD~2", "3rd on master reword");

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            // No common ancestor, throws an exception
            gitFastReword.reword(commitsToReword);
        }
    }


    @Test
    public void rewordOrphanTest()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
//        On branch 'master'
//
//        * (HEAD -> master) 3rd on master
//        *   Merge branch 'orphan_b'
//        |\
//        | * (orphan_b) 2nd on orphan_b  ->[reword]->"2nd on orphan_b reword"
//        | * 1st on orphan_b
//        * 3rd on master
//        * 2nd on master
//        * 1st on master

        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.MERGED_ORPHAN, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        Map<String, String> commitsToReword = new HashMap<>();
        commitsToReword.put("HEAD~1^2", "2nd on orphan_b reword");

        List<String> orphanBranchMessagesBeforeReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setName("orphan_b").call();
            git.log().call().forEach(commit -> orphanBranchMessagesBeforeReword.add(commit.getFullMessage()));

            git.checkout().setName("master").call();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword(commitsToReword);
        }

        List<String> orphanBranchMessagesAfterReword = new ArrayList<>();
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setName("orphan_b").call();
            git.log().call().forEach(commit -> orphanBranchMessagesAfterReword.add(commit.getFullMessage()));

            git.checkout().setName("master").call();

            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository)) {
                for (Map.Entry<String, String> mapEntry : commitsToReword.entrySet()) {
                    String expectedMsg = mapEntry.getValue();
                    String actualMsg = revWalk.parseCommit(repository.resolve(mapEntry.getKey())).getFullMessage();
                    Assert.assertEquals(expectedMsg, actualMsg);
                }
            }

            repository.close();
        } catch (Exception e) {
            Assert.fail("Error while reading test repo after reword");
            return;
        }

        Assert.assertEquals(orphanBranchMessagesBeforeReword, orphanBranchMessagesAfterReword);
    }

    @Test
    public void rewordDfsStopOnCommitTimestampTest()
            throws IOException, RepositoryNotFoundException, RepositoryNotOpenedException, GitOperationFailureException {
        try (Git git = Git.cloneRepository().setURI(MY_EXISTING_REPO_URI).setDirectory(tempRepoDir).call()) {
            git.checkout().setName("master").call();
        } catch (Exception e) {
            Assert.fail("Error while cloning test repo");
            return;
        }

        final String commitHash = "d0dd265e92e28924122fcd5841fa5e8c1f52e814";
        final String commitMessage = "Add ScratchProjectionMaths::CScratchProjectionBuilder and reword a commit!";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream, true);
        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.setInfoPrintStream(printStream);
            gitFastReword.setErrPrintStream(printStream);
            gitFastReword.openRepository(tempRepoDir.toPath());
            gitFastReword.reword(commitHash, commitMessage);
        }

        // GitFastReword will find parent of 6788ba863f8388a19de7c09d2f7f404eb4e132fc and use it as 'onto' for rebase
        // After it will stop dfs on all commits older than 'onto', the first one is 2c027b8e6b0dcd673a4166faa99c4dd95e11f496
        String expectedLogBeginning = "[ Info ] rebase (start): checkout 6788ba863f8388a19de7c09d2f7f404eb4e132fc" +
                System.lineSeparator() + "[ Info ] rebase (reset): '2c027b8e6b0dcd673a4166faa99c4dd95e11f496'";
        String actualLogBeginning = byteArrayOutputStream.toString().substring(0, expectedLogBeginning.length());
        Assert.assertEquals(expectedLogBeginning, actualLogBeginning);
    }

    @Test
    public void rewordNullCommitIdentValue()
            throws RepositoryNotOpenedException, GitOperationFailureException, IOException, RepositoryNotFoundException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        ObjectId headIdBeforeReword;
        try (Git git = Git.open(repoPath.toFile())) {
            headIdBeforeReword = git.getRepository().resolve(Constants.HEAD);
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        String commitIdent = null;
        String commitMessage = "SomeMessage";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream errPrintStream = new PrintStream(byteArrayOutputStream, true);
        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.setErrPrintStream(errPrintStream);
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword(commitIdent, commitMessage);
        }

        ObjectId headIdAfterReword;
        try (Git git = Git.open(repoPath.toFile())) {
            headIdAfterReword = git.getRepository().resolve(Constants.HEAD);
        }

        Assert.assertFalse(byteArrayOutputStream.toString().trim().isEmpty());
        Assert.assertEquals(headIdBeforeReword, headIdAfterReword);
    }

    @Test
    public void rewordNullCommitMessage() throws IOException, RepositoryNotFoundException, RepositoryNotOpenedException, GitOperationFailureException {
        Path repoPath = GitRepositoryFactory.create(GitRepositoryFactory.RepoTypes.ONE_BRANCH_FIVE_COMMITS, tempRepoDir);
        Assert.assertNotNull("Repository creation unsuccessful", repoPath);

        ObjectId headIdBeforeReword;
        try (Git git = Git.open(repoPath.toFile())) {
            headIdBeforeReword = git.getRepository().resolve(Constants.HEAD);
        } catch (Exception e) {
            Assert.fail("Error while reading test repo before reword");
            return;
        }

        String commitIdent = "HEAD";
        String commitMessage = null;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream errPrintStream = new PrintStream(byteArrayOutputStream, true);
        try (GitFastReword gitFastReword = new GitFastReword()) {
            gitFastReword.setErrPrintStream(errPrintStream);
            gitFastReword.openRepository(repoPath);
            gitFastReword.reword(commitIdent, commitMessage);
        }

        ObjectId headIdAfterReword;
        try (Git git = Git.open(repoPath.toFile())) {
            headIdAfterReword = git.getRepository().resolve(Constants.HEAD);
        }

        Assert.assertFalse(byteArrayOutputStream.toString().trim().isEmpty());
        Assert.assertEquals(headIdBeforeReword, headIdAfterReword);
    }
}
