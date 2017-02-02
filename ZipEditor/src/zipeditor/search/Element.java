/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

import zipeditor.model.Node;

public class Element extends WorkbenchAdapter implements IAdaptable {

	private Set fNodes;
	private String fPath;
	private String fFileName;
	private List fChildren;

	public Element(String path, String fileName) {
		fPath = path;
		fFileName = fileName;
	}

	public void addChild(Element child) {
		if (fChildren == null)
			fChildren = new ArrayList();
		fChildren.add(child);
	}

	public void addNode(Node node) {
		if (fNodes == null)
			fNodes = new HashSet();
		fNodes.add(node);
	}

	public List getChildren() {
		return fChildren;
	}

	public String getFileName() {
		return fFileName;
	}

	public String getPath() {
		return fPath;
	}

	public Collection getNodes() {
		return fNodes;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return this;
		return null;
	}

	public String getLabel(Object object) {
		return fFileName;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fFileName == null) ? 0 : fFileName.hashCode());
		result = prime * result + ((fPath == null) ? 0 : fPath.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Element other = (Element) obj;
		if (fFileName == null) {
			if (other.fFileName != null)
				return false;
		} else if (!fFileName.equals(other.fFileName))
			return false;
		if (fPath == null) {
			if (other.fPath != null)
				return false;
		} else if (!fPath.equals(other.fPath))
			return false;
		return true;
	}
}
