package shchuko.git_fast_reword;

/**
 * @author Vladislav Yaroahshchuk (yaroshchuk2000@gmail.com)
 */
public class GitOperationFailureException extends Exception {
    public GitOperationFailureException() {
        super();
    }

    public GitOperationFailureException(String message) {
        super(message);
    }

    public GitOperationFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitOperationFailureException(Exception e) {
        super(e);
    }
}
