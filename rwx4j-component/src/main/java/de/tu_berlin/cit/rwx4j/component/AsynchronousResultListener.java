package de.tu_berlin.cit.rwx4j.component;

import de.tu_berlin.cit.rwx4j.rest.RestDocument;

public interface AsynchronousResultListener {

	public void processResult(RestDocument doc);
	
}
