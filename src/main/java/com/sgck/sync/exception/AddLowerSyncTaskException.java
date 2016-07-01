package com.sgck.sync.exception;

@SuppressWarnings("serial")
public class AddLowerSyncTaskException extends Exception {
	public AddLowerSyncTaskException() {
		super();
	}

	/**
	 * Constructs an <code>InterruptedException</code> with the specified detail
	 * message.
	 * 
	 * @param s
	 *            the detail message.
	 */
	public AddLowerSyncTaskException(String s) {
		super(s);
	}
	
	public AddLowerSyncTaskException(String s, Throwable cause) {
		super(s,cause);
	}
}
