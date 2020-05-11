package shchuko.git_fast_reword;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;

import java.io.File;
import java.nio.file.Path;

/**
 * @author Vladislav Yaroahshchuk (yaroshchuk2000@gmail.com)
 */
public class GitRepositoryFactory {
    public enum RepoTypes {
        EMPTY,
        HEAD_DETACHED,
        ONE_BRANCH_FIVE_COMMITS,
        NOT_MERGED_BRANCHES,
        MERGED_BRANCHES,
        MERGED_ORPHAN
    }

/* - EMPTY -
   [empty repository]
*/

/* - HEAD_DETACHED -
//   * (master) Commit 2
//   * (HEAD) Commit 1
 */

/* - ONE_BRANCH_FIVE_COMMITS -
//   * (HEAD -> master) Commit 5
//   * Commit 4
//   * Commit 3
//   * Commit 2
//   * Commit 1
 */

/* - NOT_MERGED_BRANCHES -
    * (HEAD -> master) 4th on master
    | * (b2) 2nd on b2
    | * 1st on b2
    |/
    * 3rd on master
    | * (b1) 2nd on b1
    | * 1st on b1
    |/
    * 2nd on master
    * 1st on master
*/

/* - MERGED_BRANCHES -
    * (HEAD -> master) Merge branch 'b1'
    |\
    | * (b1) 2nd on b1
    | * 1st on b1
    * | 4th on master
    | | * (b2) 2nd on b2
    | | * 1st on b2
    | |/
    |/|
    * | 3rd on master
    |/
    * 2nd on master
    * 1st on master
*/

/* - MERGED_ORPHAN -
    * (HEAD -> master) 3rd on master
    *   Merge branch 'orphan_b'
    |\
    | * (orphan_b) 2nd on orphan_b
    | * 1st on orphan_b
    * 3rd on master
    * 2nd on master
    * 1st on master
*/

    public static Path create(RepoTypes repoType, File root) {
        switch (repoType) {
            case EMPTY:
                return getBlankRepo(root);
            case HEAD_DETACHED:
                return getDetachedHeadRepo(root);
            case ONE_BRANCH_FIVE_COMMITS:
                return getOneBranchFiveCommitsRepo(root);
            case NOT_MERGED_BRANCHES:
                return getNotMergedBranchesRepo(root);
            case MERGED_BRANCHES:
                return getMergedBranchesRepo(root);
            case MERGED_ORPHAN:
                return getMergedOrphanRepo(root);
            default:
                return null;
        }
    }

    private static Path getBlankRepo(File root) {
        if (root == null) {
            return null;
        }

        try {
            Git.init().setDirectory(root).call().close();
        } catch (Exception e) {
            return null;
        }
        return root.toPath();
    }

    private static Path getDetachedHeadRepo(File root) {
        if (root == null) {
            return null;
        }

        try (Git git = Git.init().setDirectory(root).call()) {
            git.commit().setAllowEmpty(true).setMessage("Commit 1").call();
            git.commit().setAllowEmpty(true).setMessage("Commit 2").call();
            ObjectId prevCommitId = git.getRepository().resolve("HEAD~1");
            git.checkout().setName(prevCommitId.getName()).call();
        } catch (Exception e) {
            return null;
        }
        return root.toPath();
    }

    private static Path getOneBranchFiveCommitsRepo(File root) {
        final int COMMIT_COUNT = 5;
        if (root == null) {
            return null;
        }

        try (Git git = Git.init().setDirectory(root).call()) {
            for (int i = 0; i < COMMIT_COUNT; ++i) {
                git.commit().setAllowEmpty(true).setMessage("Commit " + i).call();
            }
        } catch (Exception e) {
            return null;
        }
        return root.toPath();
    }

    private static Path getNotMergedBranchesRepo(File root) {
        if (root == null) {
            return null;
        }

        try (Git git = Git.init().setDirectory(root).call()) {
            git.commit().setAllowEmpty(true).setMessage("1st on master").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on master").call();

            git.checkout().setCreateBranch(true).setName("b1").call();
            git.commit().setAllowEmpty(true).setMessage("1st on b1").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on b1").call();

            git.checkout().setCreateBranch(false).setName("master").call();
            git.commit().setAllowEmpty(true).setMessage("3rd on master").call();

            git.checkout().setCreateBranch(true).setName("b2").call();
            git.commit().setAllowEmpty(true).setMessage("1st on b2").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on b2").call();

            git.checkout().setCreateBranch(false).setName("master").call();
            git.commit().setAllowEmpty(true).setMessage("4th on master").call();
        } catch (Exception e) {
            return null;
        }
        return root.toPath();
    }

    private static Path getMergedBranchesRepo(File root) {
        if (root == null) {
            return null;
        }

        try (Git git = Git.init().setDirectory(root).call()) {
            git.commit().setAllowEmpty(true).setMessage("1st on master").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on master").call();

            git.checkout().setCreateBranch(true).setName("b1").call();
            git.commit().setAllowEmpty(true).setMessage("1st on b1").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on b1").call();

            git.checkout().setCreateBranch(false).setName("master").call();
            git.commit().setAllowEmpty(true).setMessage("3rd on master").call();

            git.checkout().setCreateBranch(true).setName("b2").call();
            git.commit().setAllowEmpty(true).setMessage("1st on b2").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on b2").call();

            git.checkout().setCreateBranch(false).setName("master").call();
            git.commit().setAllowEmpty(true).setMessage("4th on master").call();

            ObjectId b1Id = git.getRepository().resolve("b1");
            git.merge().setMessage("Merge branch 'b1'").include(b1Id).call();
        } catch (Exception e) {
            return null;
        }
        return root.toPath();
    }

    private static Path getMergedOrphanRepo(File root) {
        if (root == null) {
            return null;
        }

        try (Git git = Git.init().setDirectory(root).call()) {
            git.commit().setAllowEmpty(true).setMessage("1st on master").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on master").call();
            git.commit().setAllowEmpty(true).setMessage("3rd on master").call();

            git.checkout().setOrphan(true).setName("orphan_b").call();
            git.commit().setAllowEmpty(true).setMessage("1st on orphan_b").call();
            git.commit().setAllowEmpty(true).setMessage("2nd on orphan_b").call();

            git.checkout().setCreateBranch(false).setName("master").call();
            ObjectId orphanId = git.getRepository().resolve("orphan_b");
            git.merge().setStrategy(MergeStrategy.RECURSIVE).setMessage("Merge branch 'orphan_b'").include(orphanId).call();

            git.commit().setAllowEmpty(true).setMessage("3rd on master").call();
        } catch (Exception e) {
            return null;
        }
        return root.toPath();
    }
}
