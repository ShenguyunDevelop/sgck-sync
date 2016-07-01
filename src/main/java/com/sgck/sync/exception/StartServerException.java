package com.sgck.sync.exception;

@SuppressWarnings("serial")
public class StartServerException extends Exception {
	public StartServerException() {
		super();
	}

	/**
	 * Constructs an <code>InterruptedException</code> with the specified detail
	 * message.
	 * 
	 * @param s
	 *            the detail message.
	 */
	public StartServerException(String s) {
		super(s);
	}
	
	public StartServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
