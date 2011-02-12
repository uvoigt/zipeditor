/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import zipeditor.Messages;
import zipeditor.PreferenceConstants;
import zipeditor.Utils;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.IModelListener.ModelChangeEvent;

public class ZipModel {
	protected class SevenZipCreator {
		private RandomAccessFile raf;
		private ISevenZipInArchive archive;
		private ArchiveFormat format;
		private ISimpleInArchive simpleInterface;
		protected Object entry;
		private int itemCount;
		private int cursor;

		public SevenZipCreator() {
			openArchive(0);
		}
		boolean isOpen() {
			try {
				return raf.getFD().valid();
			} catch (IOException e) {
				return false;
			}
		}
		ISimpleInArchiveItem openArchive(int index) {
			try {
				raf = new RandomAccessFile(zipPath, "rw"); //$NON-NLS-1$
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			IInStream stream = new RandomAccessFileInStream(raf);
			archive = detectSevenZip(stream);
			if (archive == null) {
				try {
					raf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
			format = archive.getArchiveFormat();
			simpleInterface = archive.getSimpleInterface();
			if (ZipEditorPlugin.DEBUG)
				System.out.println("Detected type (" + zipPath.getName() + "): " + format); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				itemCount = simpleInterface.getNumberOfItems();
//				archive.close();
				return simpleInterface.getArchiveItem(index);
			} catch (SevenZipException e1) {
				e1.printStackTrace();
				return null;
			}
		}
		private ISevenZipInArchive detectSevenZip(IInStream in) {
			try {
				return SevenZip.openInArchive(null, in);
			} catch (SevenZipException e) {
				System.err.println(e);
				return null;
			}
		}
		void nextEntry() throws IOException {
			try {
				entry = itemCount > cursor ? simpleInterface.getArchiveItem(cursor++) : null;
			} catch (SevenZipException e) {
				throw new IOException(e);
			}
		}
		String getEntryName() {
			try {
				return ((ISimpleInArchiveItem) entry).getPath();
			} catch (SevenZipException e) {
				ZipEditorPlugin.log(e);
				return null;
			}
		}
		long entrySize() {
			try {
				return ((ISimpleInArchiveItem) entry).getSize() != null ? ((ISimpleInArchiveItem) entry).getSize().longValue() : 0;
			} catch (SevenZipException e) {
				ZipEditorPlugin.log(e);
				return 0;
			}
		}
		boolean isDirectory() {
			try {
				return ((ISimpleInArchiveItem) entry).isFolder();
			} catch (SevenZipException e) {
				ZipEditorPlugin.log(e);
				return false;
			}
		}
		Node createNode(String name, boolean isFolder) {
			return new SevenZipNode(ZipModel.this, (ISimpleInArchiveItem) entry,
					this, name, isFolder);
		}
		void close() throws IOException {
			try {
				archive.close();
			} catch (SevenZipException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			raf.close();
		}
	}

	public final static int ZIP = 1;
	public final static int TAR = 2;
	public final static int GZ = 3;
	public final static int TARGZ = 4;
	public final static int EMPTY = 99;

	public final static int INIT_STARTED = 0x01;
	public final static int INIT_FINISHED = 0x02;
	public final static int INITIALIZING = 0x04;
	public final static int DIRTY = 0x08;

	public static int typeFromName(String string) {
		if (string != null) {
			String lowerCase = string.toLowerCase();
			if (lowerCase.endsWith(".tgz") || lowerCase.endsWith(".tar.gz")) //$NON-NLS-1$ //$NON-NLS-2$
				return TARGZ;
			if (lowerCase.endsWith(".gz")) //$NON-NLS-1$
				return GZ;
			if (lowerCase.endsWith(".tar")) //$NON-NLS-1$
				return TAR;
		}
		return ZIP;
	}

	public static ArchiveFormat detectType(InputStream contents) {
		try {
			File tempFile = File.createTempFile("zip", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
			Utils.readAndWrite(contents, new FileOutputStream(tempFile), true, false);
			ZipModel model = new ZipModel(tempFile, contents, true);
			SevenZipCreator creator = model.new SevenZipCreator();
			creator.close();
			tempFile.delete();
			return creator.format;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected Node root;
	private File zipPath;
	private int state;
	private int type;
	private File tempDir;
	private boolean readonly;
	private ListenerList listenerList = new ListenerList();
	
	public ZipModel(File path, final InputStream inputStream, boolean readonly) {
		zipPath = path;
		this.readonly = readonly;
		state |= INITIALIZING;
		if (path != null && path.length() >= 10000000) {
			Thread initThread = new Thread(Messages.getFormattedString("ZipModel.0", path.getName())) { //$NON-NLS-1$
				public void run() {
					initialize(inputStream);
				}
			};
			initThread.start();
		} else {
			initialize(inputStream);
		}
	}
	
	private void initialize(InputStream inputStream) {
		long time = System.currentTimeMillis();
		SevenZipCreator creator = null;
		try {
			root = new SevenZipNode(this, new String(), true);
			creator = new SevenZipCreator();
			readStream(inputStream, creator);
		} finally {
			if (creator != null) {
				try {
					creator.close();
				} catch (IOException e) {
					ZipEditorPlugin.log(e);
				}
			}
			state &= -1 ^ INITIALIZING;
			state |= INIT_FINISHED;
			notifyListeners();
			state &= -1 ^ INIT_FINISHED;
			if (ZipEditorPlugin.DEBUG)
				System.out.println(zipPath + " initialized in " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private void readStream(InputStream zipStream, SevenZipCreator creator) {
		state |= INIT_STARTED;
		while (true) {
			if (!isInitializing()) {
				state |= DIRTY;
				break;
			}
			try {
				creator.nextEntry();
			} catch (Exception e) {
				ZipEditorPlugin.log(e);
				break;
			}
			if ((creator.entry == null)
					/*|| (root.children != null)*/) {
				state &= -1 ^ DIRTY;
				break;
			}
			String entryName = creator.getEntryName();
			if (entryName == null)
				entryName = zipPath.getName().endsWith(".gz") ? zipPath.getName().substring(0, //$NON-NLS-1$
								zipPath.getName().length() - 3)
						: zipPath.getName();
			String[] names = splitName(entryName);
			Node node = null;
			int n = names.length - 1;
			for (int i = 0; i < n; i++) {
				String pathSeg = names[i];
				Node parent = node != null ? node : root;
				node = parent.getChildByName(pathSeg, false);
				if (node == null) {
					parent.add(node = parent.create(this, pathSeg, true), null);
					node.time = -1;
				}
			}
			boolean isFolder = entryName.endsWith("/") || entryName.endsWith("\\") || //$NON-NLS-1$ //$NON-NLS-2$
					creator.isDirectory();
			if (node == null)
				node = root;
			Node existingNode = n == -1 ? null : node.getChildByName(names[n], false);
			if (existingNode != null) {
				existingNode.update(creator.entry);
			} else {
				String name = n >= 0 ? names[n] : "/"; //$NON-NLS-1$
				Node newChild = creator.createNode(name, isFolder); 
				node.add(newChild, null);
				if (zipPath == null) {
					byte[] buf = new byte[8000];
					ByteArrayOutputStream out = null;
					try {
						for (int count = 0; (count = zipStream.read(buf)) != -1; ) {
							if (out == null)
								out = new ByteArrayOutputStream();
							out.write(buf, 0, count);
						}
					} catch (Exception e) {
						ZipEditorPlugin.log(e);
					}
					if (out != null)
						newChild.setContent(out.toByteArray());
				}
				if (zipStream instanceof ZipInputStream) {
					try {
						((ZipInputStream) zipStream).closeEntry();
					} catch (Exception e) {
						ZipEditorPlugin.log(e);
					}
				}
				newChild.setSize(creator.entrySize());
			}
			state &= -1 ^ INIT_STARTED;
		}
	}
	
	public InputStream save(int type, IProgressMonitor monitor) throws IOException {
		File tmpFile = new File(root.getModel().getTempDir(), Integer.toString((int) System.currentTimeMillis()));
		OutputStream out = new FileOutputStream(tmpFile);
		try {
			switch (type) {
			case GZ:
				out = new GZIPOutputStream(out);
				break;
			case TAR:
				out = new TarOutputStream(out);
				break;
			case TARGZ:
				out = new TarOutputStream(new GZIPOutputStream(out));
				break;
			case ZIP:
				out = new ZipOutputStream(out);
				break;
			}
			if (out instanceof TarOutputStream)
				((TarOutputStream) out).setLongFileMode(TarOutputStream.LONGFILE_GNU);
			boolean storeFolders = ZipEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.STORE_FOLDERS_IN_ARCHIVES);
			saveNodes(out, root, type, storeFolders, monitor);
		} catch (Exception e) {
			ZipEditorPlugin.log(e);
		} finally {
			out.close();
		}
		return new FileInputStream(tmpFile);
	}

	private void saveNodes(OutputStream out, Node node, int type, boolean storeFolders, IProgressMonitor monitor) throws IOException {
		if (out == null)
			return;
		Node[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (monitor.isCanceled())
				break;
			Node child = children[i];
			String entryName = child.getPath() + child.getName();
			if (child.isFolder()) {
				saveNodes(out, child, type, storeFolders, monitor);
				if (!storeFolders)
					continue;
				entryName = child.getPath();
			}
			ZipEntry zipEntry = type == ZIP ? new ZipEntry(entryName) : null;
			TarEntry tarEntry = type == TAR || type == TARGZ ? new TarEntry(entryName): null;
			if (zipEntry != null) {
				zipEntry.setTime(child.getTime());
				if (child instanceof ZipNode)
					zipEntry.setComment(((ZipNode) child).getComment());
			} else if (tarEntry != null) {
				tarEntry.setModTime(child.getTime());
				tarEntry.setSize(child.getSize());
				if (child instanceof TarNode) {
					TarNode tarNode = (TarNode) child;
					tarEntry.setGroupId(tarNode.getGroupId());
					tarEntry.setGroupName(tarNode.getGroupName());
					tarEntry.setUserId(tarNode.getUserId());
					tarEntry.setUserName(tarNode.getUserName());
					tarEntry.setGroupId(tarNode.getGroupId());
					tarEntry.setMode(tarNode.getMode());
				} else {
					tarEntry.setMode(TarEntry.DEFAULT_FILE_MODE);
				}
			}
			
			if (out instanceof ZipOutputStream)
				((ZipOutputStream) out).putNextEntry(zipEntry);
			else if (out instanceof TarOutputStream)
				((TarOutputStream) out).putNextEntry(tarEntry);
			Utils.readAndWrite(child.getContent(), out, false);
			if (tarEntry != null)
				((TarOutputStream) out).closeEntry();
			monitor.worked(1);
		}
	}

	protected String[] splitName(String name) {
		List list = new ArrayList();
		while (name != null && name.length() > 0) {
			int index = name.indexOf('/');
			if (index == -1)
				index = name.indexOf('\\');
			if (index != -1) {
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
		ModelChangeEvent event = new ModelChangeEvent(this);
		for (int i = 0; i < listeners.length; i++) {
			((IModelListener) listeners[i]).modelChanged(event);
		}
	}

	public void dispose() {
		state &= -1 ^ INITIALIZING;
		deleteTempDir(tempDir);
		tempDir = null;
		ZipEditorPlugin.getDefault().removeFileMonitors(this);
	}
	
	private void deleteTempDir(final File tmpDir) {
		if (deleteFile(tmpDir))
			return;
		Job job = new Job("Deleting temporary directory") { //$NON-NLS-1$
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Waiting for accessing tasks to be finished", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
				do {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
					}
					if (monitor.isCanceled())
						break;
				} while (!deleteFile(tmpDir));
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
	}

	boolean deleteFile(File file) {
		if (file == null)
			return true;
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i]);
				}
			}
		}
		boolean success = file.delete();
		if (!success)
			System.out.println("Couldn't delete " + file); //$NON-NLS-1$
		return success;
	}

	public File getTempDir() {
		if (tempDir == null) {
			File sysTmpDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			tempDir = new File(sysTmpDir, "zip" + (int) System.currentTimeMillis()); //$NON-NLS-1$
			tempDir.mkdirs();
		}
		return tempDir;
	}

	public Node getRoot() {
		return root;
	}
	
	public int getType() {
		return type;
	}
	
	int getState() {
		return state;
	}

	public boolean isInitializing() {
		return (state & INITIALIZING) > 0;
	}

	public boolean isDirty() {
		return (state & DIRTY) > 0;
	}
	
	public void setDirty(boolean dirty) {
		if (dirty) {
			if (!isInitializing())
				state |= DIRTY;
		} else {
			state &= -1 ^ DIRTY;
		}
	}
	
	public boolean isReadonly() {
		return readonly;
	}

	public File getZipPath() {
		return zipPath;
	}
	
	public Node findNode(String path) {
		String[] names = splitName(path);
		Node node = root;
		for (int i = 0; i < names.length && node != null; i++) {
			node = node.getChildByName(names[i], false);
		}
		return node;
	}
}
