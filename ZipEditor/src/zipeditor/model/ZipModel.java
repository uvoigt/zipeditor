/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.ListenerList;

import zipeditor.ZipEditorPlugin;

public class ZipModel {
	private ZipNode root;
	private File zipPath;
	private File tempDir;
	private List tempFilesToDelete;
	private ListenerList listenerList = new ListenerList();

	public ZipModel(File path, InputStream in) {
		zipPath = path;
		ZipInputStream zip = new ZipInputStream(in);
		ZipEntry entry = null;
		root = new ZipNode(this, null, "", true); //$NON-NLS-1$
		while (true) {
			try {
				entry = zip.getNextEntry();
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
				break;
			}
			if (entry == null)
				break;
			String entryName = entry.getName();
			String[] names = splitName(entryName);
			ZipNode node = null;
			int n = names.length - 1;
			for (int i = 0; i < n; i++) {
				String pathSeg = names[i];
				ZipNode parent = node != null ? node : root;
				node = parent.getChildByName(pathSeg, true);
				if (node == null)
					parent.add(node = new ZipNode(this, null, pathSeg, true));
			}
			boolean isFolder = entryName.endsWith("/") || entryName.endsWith("\\") || entry.isDirectory(); //$NON-NLS-1$ //$NON-NLS-2$
			if (node == null)
				node = root;
			if (node.getChildByName(names[n], true) == null) {
				ZipNode newChild = new ZipNode(this, entry, names[n], isFolder);
				node.add(newChild);
				try {
					zip.closeEntry();
				} catch (Exception e) {
					ZipEditorPlugin.log(e);
				}
				newChild.setSize(entry.getSize());
			}
			
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				ZipEditorPlugin.log(e);
			}
		}
	}
	
	private String[] splitName(String name) {
		List list = new ArrayList();
		while (name.length() > 0) {
			int index = name.indexOf('/');
			if (index == -1)
				index = name.indexOf('\\');
			if (index != -1) {
				if (index > 0)
					list.add(name.substring(0, index));
				name = name.substring(index + 1);
			} else {
				list.add(name);
				name = new String();
			}
		}
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	public void addModelListener(IModelListener listener) {
		listenerList.add(listener);
	}
	
	public void removeModelListener(IModelListener listener) {
		listenerList.remove(listener);
	}
	
	protected void notifyListeners() {
		Object[] listeners = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((IModelListener) listeners[i]).modelChanged();
		}
	}

	public void dispose() { 
		if (tempFilesToDelete != null) {
			for (int i = 0, n = tempFilesToDelete.size(); i < n; i++) {
				deleteFile(((File) tempFilesToDelete.get(i)));
			}
			tempFilesToDelete.clear();
			tempFilesToDelete = null;
		}
		tempDir = null;
	}

	public void deleteFileOnDispose(File file) {
		if (file == null)
			return;
		if (tempFilesToDelete == null)
			tempFilesToDelete = new ArrayList();
		tempFilesToDelete.add(file);
	}
	
	private void deleteFile(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i]);
				}
			}
		}
		if (!file.delete())
			System.out.println("Couldn't delete " + file); //$NON-NLS-1$
	}

	public File getTempDir() {
		if (tempDir == null) {
			File sysTmpDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			tempDir = new File(sysTmpDir, "zip" + (int) System.currentTimeMillis()); //$NON-NLS-1$
			tempDir.mkdirs();
			deleteFileOnDispose(tempDir);
		}
		return tempDir;
	}

	public ZipNode getRoot() {
		return root;
	}

	public ZipFile getZipFile() throws IOException {
		return new ZipFile(zipPath);
	}
	
	public File getZipPath() {
		return zipPath;
	}

}
