package osgi.enroute.rest.api;

public class BadRequest400Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public BadRequest400Exception() {
        super();
    }

    public BadRequest400Exception( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

    public BadRequest400Exception( String message, Throwable cause ) {
        super( message, cause );
    }

    public BadRequest400Exception( String message ) {
        super( message );
    }

    public BadRequest400Exception( Throwable cause ) {
        super( cause );
    }
}
