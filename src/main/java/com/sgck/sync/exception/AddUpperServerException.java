package com.sgck.sync.exception;

@SuppressWarnings("serial")
public class AddUpperServerException extends Exception {
	public AddUpperServerException() {
		super();
	}

	/**
	 * Constructs an <code>InterruptedException</code> with the specified detail
	 * message.
	 * 
	 * @param s
	 *            the detail message.
	 */
	public AddUpperServerException(String s) {
		super(s);
	}
	
	public AddUpperServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
