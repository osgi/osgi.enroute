package osgi.enroute.rest.api;

public class Forbidden403Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public Forbidden403Exception() {
        super();
    }

    public Forbidden403Exception( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

    public Forbidden403Exception( String message, Throwable cause ) {
        super( message, cause );
    }

    public Forbidden403Exception( String message ) {
        super( message );
    }

    public Forbidden403Exception( Throwable cause ) {
        super( cause );
    }
}
