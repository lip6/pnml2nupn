package fr.lip6.move.pnml2bpn.exceptions;

public class InvalidNetException extends Exception {

	private static final long serialVersionUID = 7768156446274359970L;

	public InvalidNetException() {
		super();
	}

	public InvalidNetException(String message) {
		super(message);
	}

	public InvalidNetException(Throwable cause) {
		super(cause);
	}

	public InvalidNetException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidNetException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
