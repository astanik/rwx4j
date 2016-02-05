package de.tu_berlin.cit.rwx4j.plugin;

import de.tu_berlin.cit.rwx4j.container.ResourceInstance;
import de.tu_berlin.cit.rwx4j.xwadl.XwadlDocument;

public interface IContainerPlugin {

	public XwadlDocument extendXwadl(XwadlDocument xwadl, String path, ResourceInstance instance);
	
}
