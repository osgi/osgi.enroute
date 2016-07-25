package osgi.enroute.rest.api;

public class MethodNotAllowed405Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public MethodNotAllowed405Exception() {
        super();
    }

    public MethodNotAllowed405Exception( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }

    public MethodNotAllowed405Exception( String message, Throwable cause ) {
        super( message, cause );
    }

    public MethodNotAllowed405Exception( String message ) {
        super( message );
    }

    public MethodNotAllowed405Exception( Throwable cause ) {
        super( cause );
    }
}
