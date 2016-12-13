/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import zipeditor.model.Node;

public class ZipSearchResultCollector {

	private IProgressMonitor fMonitor;

	private ZipSearchResult fResult;

    public ZipSearchResultCollector(ZipSearchResult result) {
    	fResult = result;
	}

	public void setProgressMonitor(IProgressMonitor monitor) {
		fMonitor = monitor;		
	}

	public IProgressMonitor getfMonitor() {
		return fMonitor;
	}

	public void accept(List parentNodes, Node node, boolean onNodeName, int offset, int length) {
		if (node.getParentNodes() == null)
			node.setParentNodes(new ArrayList(parentNodes));
		fResult.addMatch(new ZipMatch(node, onNodeName, offset, length));
	}
}
