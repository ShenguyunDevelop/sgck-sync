package com.sgck.sync.exception;

@SuppressWarnings("serial")
public class AddLowerInvokeTaskException extends Exception {
	public AddLowerInvokeTaskException() {
		super();
	}

	/**
	 * Constructs an <code>InterruptedException</code> with the specified detail
	 * message.
	 * 
	 * @param s
	 *            the detail message.
	 */
	public AddLowerInvokeTaskException(String s) {
		super(s);
	}
	
	public AddLowerInvokeTaskException(String s, Throwable cause) {
		super(s,cause);
	}
}
