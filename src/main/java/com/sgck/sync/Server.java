package com.sgck.sync;

import com.sgck.sync.exception.StartServerException;

public interface Server {
	public void start() throws StartServerException;
	public void terminate();
	public void drop();
}
