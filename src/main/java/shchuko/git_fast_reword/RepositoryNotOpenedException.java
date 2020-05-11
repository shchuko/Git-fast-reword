package shchuko.git_fast_reword;

/**
 * @author Vladislav Yaroahshchuk (yaroshchuk2000@gmail.com)
 */
public class RepositoryNotOpenedException extends Exception {
    public RepositoryNotOpenedException() {
        super();
    }

    public RepositoryNotOpenedException(String message) {
        super(message);
    }

    public RepositoryNotOpenedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryNotOpenedException(Exception e) {
        super(e);
    }
}
