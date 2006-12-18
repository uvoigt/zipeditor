package zipeditor.model;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;

import zipeditor.Utils;
import zipeditor.operations.ExtractOperation;

public class FileAdapter implements IAdaptable {
	private Node fNode;
	private IFileStore fFileStore;
	
	public FileAdapter(Node node) {
		if (node == null)
			throw new NullPointerException();
		fNode = node;
	}

	public Object getAdapter(Class adapter) {
		if (IFileStore.class.equals(adapter)) {
			if (fFileStore == null)
				fFileStore = extractNode();
			return fFileStore; 
		} else if (Node.class.equals(adapter))
			return fNode;
		return null;
	}
	
	public boolean isAdapted() {
		return fFileStore != null;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FileAdapter))
			return false;
		return fNode.equals(((FileAdapter) obj).fNode);
	}

	private IFileStore extractNode() {
		ExtractOperation operation = new ExtractOperation();
		File path = operation.extract(fNode, fNode.getModel().getTempDir(), true, new NullProgressMonitor());
		return Utils.getFileStore(path);
	}

}
