package com.sgck.sync.exception;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class AddUpperInvokeTaskException extends Exception {
	private List<String> brokenUpperUrls = null;
	
	public AddUpperInvokeTaskException() {
		super();
	}

	/**
	 * Constructs an <code>InterruptedException</code> with the specified detail
	 * message.
	 * 
	 * @param s
	 *            the detail message.
	 */
	public AddUpperInvokeTaskException(String s) {
		super(s);
	}

	@Override
	public String getMessage() {
		if(brokenUpperUrls == null){
			return super.getMessage();
		}else{
			return "Failed to add sync task to those server:  " + brokenUpperUrls;
		}
    }

	public List<String> getBrokenUpperUrls() {
		return brokenUpperUrls;
	}

	public void setBrokenUpperUrls(List<String> brokenUpperUrls) {
		this.brokenUpperUrls = brokenUpperUrls;
	}
}
