package shchuko.git_fast_reword;

/**
 * @author Vladislav Yaroahshchuk (yaroshchuk2000@gmail.com)
 */
public class RepositoryNotFoundException extends Exception {
    public RepositoryNotFoundException() {
        super();
    }

    public RepositoryNotFoundException(String message) {
        super(message);
    }

    public RepositoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryNotFoundException(Exception e) {
        super(e);
    }
}
