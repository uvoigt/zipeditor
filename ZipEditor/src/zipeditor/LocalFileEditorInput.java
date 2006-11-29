/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor;

import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class LocalFileEditorInput implements IPathEditorInput, IStorageEditorInput, ILocationProvider {
	private class WorkbenchAdapter implements IWorkbenchAdapter {
		public Object[] getChildren(Object o) {
			return null;
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		public String getLabel(Object o) {
			return ((LocalFileEditorInput)o).getName();
		}

		public Object getParent(Object o) {
			return null;
		}
	};

	private class FileStorage implements IStorage {
		private IFileStore fFileStore;
		private IPath fFullPath;
		
		public FileStorage(IFileStore fileStore) {
			Assert.isNotNull(fileStore);
			Assert.isTrue(EFS.SCHEME_FILE.equals(fileStore.getFileSystem().getScheme()));
			fFileStore = fileStore;
		}
		
		public InputStream getContents() throws CoreException {
			return fFileStore.openInputStream(EFS.NONE, null);
		}

		public IPath getFullPath() {
	    	if (fFullPath == null)
	    		fFullPath = new Path(fFileStore.toURI().getPath());
	    	return fFullPath;
		}

		public String getName() {
			return fFileStore.getName();
		}

		public boolean isReadOnly() {
			return fFileStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY);
		}

		public Object getAdapter(Class adapter) {
			return null;
		}
	};

	private IFileStore fFileStore;
	private WorkbenchAdapter fWorkbenchAdapter = new WorkbenchAdapter();
	private IStorage fStorage;
	private IPath fPath;
	
	public LocalFileEditorInput(IFileStore fileStore) {
		Assert.isNotNull(fileStore);
		Assert.isTrue(EFS.SCHEME_FILE.equals(fileStore.getFileSystem().getScheme()));
		fFileStore = fileStore;
		fWorkbenchAdapter = new WorkbenchAdapter();
	}

	public boolean exists() {
		return fFileStore.fetchInfo().exists();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return fFileStore.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return fFileStore.toString();
	}

	public Object getAdapter(Class adapter) {
		if (ILocationProvider.class.equals(adapter))
			return this;
		if (IWorkbenchAdapter.class.equals(adapter))
			return fWorkbenchAdapter;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public IPath getPath(Object element) {
		if (element instanceof LocalFileEditorInput)
			return ((LocalFileEditorInput)element).getPath();
		
		return null;
	}

    public IPath getPath() {
    	if (fPath == null)
    		fPath = new Path(fFileStore.toURI().getPath());
    	return fPath;
    }

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof LocalFileEditorInput) {
			LocalFileEditorInput input = (LocalFileEditorInput) o;
			return fFileStore.equals(input.fFileStore);
		}

        if (o instanceof IPathEditorInput) {
            IPathEditorInput input= (IPathEditorInput)o;
            return getPath().equals(input.getPath());
        }
		return false;
	}

	public int hashCode() {
		return fFileStore.hashCode();
	}

	public IStorage getStorage() throws CoreException {
		if (fStorage == null)
			fStorage = new FileStorage(fFileStore);
		return fStorage;
	}

}
