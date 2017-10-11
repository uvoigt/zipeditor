/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

public class Element extends WorkbenchAdapter implements IAdaptable {

	private final Object fParent;
	private final String fPath;
	private final String fFileName;
	private final Long fSize;
	private final Long fLastModified;

	public Element(Object parent, String path, String fileName, Long size, Long lastModified) {
		fParent = parent;
		fPath = path;
		fFileName = fileName;
		fSize = size;
		fLastModified = lastModified;
	}

	public String getFileName() {
		return fFileName;
	}

	public Object getParent(Object object) {
		return fParent;
	}

	public String getPath() {
		return fPath;
	}

	public Long getSize() {
		return fSize;
	}

	public Long getLastModified() {
		return fLastModified;
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
		result = prime * result + ((fParent == null) ? 0 : fParent.hashCode());
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
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (!fParent.equals(other.fParent))
			return false;
		if (fPath == null) {
			if (other.fPath != null)
				return false;
		} else if (!fPath.equals(other.fPath))
			return false;
		return true;
	}
}
