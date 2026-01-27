package sad.storereg.exception;

public class InternalServerError extends RuntimeException{
	
	private static final long serialVersionUID = 1L;

    public InternalServerError(String message) {
        super(message);
    }

    public InternalServerError(String message, Throwable cause) {
        super(message, cause);
    }

}
