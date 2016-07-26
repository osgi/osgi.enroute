package osgi.enroute.rest.api;

public class NotFound404Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public NotFound404Exception() {
        super();
    }

    public NotFound404Exception( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

    public NotFound404Exception( String message, Throwable cause ) {
        super( message, cause );
    }

    public NotFound404Exception( String message ) {
        super( message );
    }

    public NotFound404Exception( Throwable cause ) {
        super( cause );
    }
}
