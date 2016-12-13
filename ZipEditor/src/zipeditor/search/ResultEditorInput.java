/*
 * (c) Copyright 2002, 2016 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import zipeditor.model.Node;

public class ResultEditorInput implements IStorageEditorInput {
	private final IStorage fStorage = new IStorage() {
		
		public Object getAdapter(Class adapter) {
			return fNode.getAdapter(adapter);
		}
		
		public boolean isReadOnly() {
			return true;
		}
		
		public String getName() {
			return fNode.getName();
		}
		
		public IPath getFullPath() {
			return null;
		}
		
		public InputStream getContents() throws CoreException {
			return fNode.getContent();
		}
	};
	private final Node fNode;

	ResultEditorInput(Node node) {
		fNode = node;
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		return obj instanceof ResultEditorInput && ((ResultEditorInput) obj).fNode.equals(fNode);
	}

	public Object getAdapter(Class adapter) {
		return fNode.getAdapter(adapter);
	}

	public String getToolTipText() {
		return ZipSearchLabelProvider.getNodeText(fNode, fNode.getParentNodes(), 0);
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getName() {
		return fNode.getName();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public boolean exists() {
		return true;
	}

	public IStorage getStorage() throws CoreException {
		return fStorage;
	}
}