/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.tar.TarUtils;
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
	public interface IErrorReporter {
		void reportError(IStatus message);
	}

	public final static int ZIP = 1;
	public final static int TAR = 2;
	public final static int GZ = 3;
	public final static int TARGZ = 4;
	public final static int BZ2 = 5;
	public final static int TARBZ2 = 6;
	public final static int EMPTY = 99;

	public final static int INIT_STARTED = 0x01;
	public final static int INIT_FINISHED = 0x02;
	public final static int INITIALIZING = 0x04;
	public final static int DIRTY = 0x08;

	/** @see: {@link org.apache.tools.tar.TarEntry#parseTarHeader(byte[])} */
	private static final int TAR_MAGIC_OFFSET = TarConstants.NAMELEN //
			+ TarConstants.MODELEN //
			+ TarConstants.UIDLEN //
			+ TarConstants.GIDLEN //
			+ TarConstants.SIZELEN //
			+ TarConstants.MODTIMELEN //
			+ TarConstants.CHKSUMLEN //
			+ 1 // linkFlag
			+ TarConstants.NAMELEN; // linkName

	public static int typeFromName(String string) {
		if (string != null) {
			String lowerCase = string.toLowerCase();
			if (lowerCase.endsWith(".tgz") || lowerCase.endsWith(".tar.gz")) //$NON-NLS-1$ //$NON-NLS-2$
				return TARGZ;
			if (lowerCase.endsWith(".gz")) //$NON-NLS-1$
				return GZ;
			if (lowerCase.endsWith(".tar")) //$NON-NLS-1$
				return TAR;
			if (lowerCase.endsWith(".tbz") || lowerCase.endsWith(".tar.bz2")) //$NON-NLS-1$ //$NON-NLS-2$
				return TARBZ2;
			if (lowerCase.endsWith(".bz2")) //$NON-NLS-1$
				return BZ2;
		}
		return ZIP;
	}

	public static int detectType(InputStream contents) {
		if (!contents.markSupported())
			contents = new BufferedInputStream(contents);
		try {
			contents.mark(1000000); // an entry which exceeds this limit cannot be detected
			int count = contents.read();
			contents.reset();
			if (count == -1)
				return EMPTY;
			ZipInputStream zip = new ZipInputStream(contents);
			if (zip.getNextEntry() != null) {
				contents.reset();
				return ZIP;
			}
			contents.reset();
			try {
				contents.skip(2);
				CBZip2InputStream bzip = new CBZip2InputStream(contents);
				if (isTarArchive(bzip)) {
					contents.reset();
					return TARBZ2;
				} else {
					contents.reset();
					return BZ2;
				}
			} catch (IOException ioe) {
				// thrown in constructor, no bzip2
			}
			contents.reset();
			try {
				GZIPInputStream gzip = new GZIPInputStream(contents);
				if (isTarArchive(gzip)) {
					contents.reset();
					return TARGZ;
				} else {
					contents.reset();
					return GZ;
				}
			} catch (IOException ioe) {
				// thrown in constructor, no gzip
			}
			contents.reset();
			
			// Es gibt gueltige Tar-Archive ohne TAR-Header, die von
			// isTarArchive nicht erkannt werden, magic ist dann leer, deshalb
			// bleibt TAR hier default
			//
			return TAR;

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean isTarArchive(InputStream bzip) throws IOException {
		byte[] tarEntryHeader = new byte[TAR_MAGIC_OFFSET + TarConstants.MAGICLEN];
		bzip.read(tarEntryHeader);
		String magic;
		try {
			magic = String.valueOf(TarUtils.parseName(tarEntryHeader, TAR_MAGIC_OFFSET, TarConstants.MAGICLEN));
		} catch (NoSuchMethodError e) {
			// http://sourceforge.net/p/zipeditor/bugs/7/
			// since Ant 1.9.0, this has been changed to String parseName(byte[] buffer, final int offset, final int length)
			try {
				Object o = TarUtils.class.getMethod("parseName", new Class[] { byte[].class, int.class, int.class }).invoke(null, //$NON-NLS-1$
						new Object[] { tarEntryHeader, Integer.valueOf(TAR_MAGIC_OFFSET), Integer.valueOf(TarConstants.MAGICLEN) });
				magic = o.toString();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return TarConstants.TMAGIC.equals(magic) || TarConstants.GNU_TMAGIC.equals(magic)
				|| (TarConstants.MAGIC_POSIX + TarConstants.VERSION_POSIX).equals(magic);
	}

	private Node root;
	private File zipPath;
	private int state;
	private int type;
	private File tempDir;
	private final boolean readonly;
	private Boolean storeFolders;
	private final ListenerList listenerList = new ListenerList();
	private final IErrorReporter errorReporter;
	private InputStream inputStream;

	public ZipModel(File path, InputStream inputStream) {
		zipPath = path;
		readonly = true;
		errorReporter = null;
		// only for one usage
		this.inputStream = inputStream;
	}

	public ZipModel(File path, final InputStream inputStream, boolean readonly) {
		this(path, inputStream, readonly, null);
	}

	public ZipModel(File path, final InputStream inputStream, boolean readonly, IErrorReporter errorReporter) {
		zipPath = path;
		this.readonly = readonly;
		this.errorReporter = errorReporter;
		state |= INITIALIZING;
		if (path != null && path.length() >= 10000000) {
			Thread initThread = new Thread(Messages.getFormattedString("ZipModel.0", path.getName())) { //$NON-NLS-1$
				public void run() {
					initialize(inputStream, null, null);
				}
			};
			initThread.start();
		} else {
			initialize(inputStream, null, null);
		}
	}
	
	public void logError(Object message) {
		IStatus status = ZipEditorPlugin.log(message);
		if (errorReporter != null)
			errorReporter.reportError(status);
	}

	public void init(IModelInitParticipant participant) {
		if (inputStream == null) {
			try {
				inputStream = new FileInputStream(zipPath);
			} catch (FileNotFoundException e) {
				logError(e);
			}
		}
		try {
			initialize(inputStream, participant, null);
		} finally {
			if (zipPath != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					logError(e);
				}
				inputStream = null;
			}
		}
	}

	private void initialize(InputStream inputStream, IModelInitParticipant participant, Node stopNode) {
		state |= INITIALIZING;
		long time = System.currentTimeMillis();
		InputStream zipStream = inputStream;
		try {
			zipStream = detectStream(inputStream);
			root = getRoot(zipStream);
			readStream(zipStream, participant, stopNode);
		} catch (IOException e) {
			// ignore
		} finally {
			if (zipStream != null && participant == null) {
				try {
					zipStream.close();
				} catch (IOException e) {
					logError(e);
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

	private Node getRoot(InputStream zipStream) throws IOException {
		if (zipStream instanceof ZipInputStream) {
			return new ZipNode(this, "", true); //$NON-NLS-1$
		}
		if (zipStream instanceof TarInputStream) {
			return new TarNode(this, "", true); //$NON-NLS-1$
		}
		if (zipStream instanceof CBZip2InputStream) {
			return new Bzip2Node(this, "", true); //$NON-NLS-1$
		}
		return new GzipNode(this, "", true); //$NON-NLS-1$
	}

	private void readStream(InputStream zipStream, IModelInitParticipant participant, Node stopNode) {
		ZipEntry zipEntry = null;
		TarEntry tarEntry = null;
		state |= INIT_STARTED;
		boolean isNoEntry = zipStream instanceof GZIPInputStream || zipStream instanceof CBZip2InputStream;
		while (true) {
			if (!isInitializing()) {
				state |= DIRTY;
				break;
			}
			try {
				if (zipStream instanceof ZipInputStream)
					zipEntry = ((ZipInputStream) zipStream).getNextEntry();
				else if (zipStream instanceof TarInputStream)
					tarEntry = ((TarInputStream) zipStream).getNextEntry();
			} catch (Exception e) {
				String message = "Error reading archive"; //$NON-NLS-1$
				if (zipPath != null)
					message += " " + zipPath.getAbsolutePath(); //$NON-NLS-1$
				logError(ZipEditorPlugin.createErrorStatus(message, e));
				break;
			}
			if ((!isNoEntry && zipEntry == null && tarEntry == null) || (isNoEntry && root.children != null)) {
				state &= -1 ^ DIRTY;
				break;
			}
			String zipName = zipPath != null ? zipPath.getName() : ""; //$NON-NLS-1$
			// handle two special cases here:
			// 1. the participant is present when an archive is scanned by the search process, the participant knows the nesting structure
			if (participant != null) {
				List parentNodes = participant.getParentNodes();
				if (isNoEntry && parentNodes != null && parentNodes.size() > 0)
					zipName = ((Node) parentNodes.get(parentNodes.size() - 1)).getName();
			}
			// 2. a nested gzip or bzip2 node is is extracted or opened from a search result, the stopNode knows its nesting structure 
			if (isNoEntry && (stopNode instanceof GzipNode || stopNode instanceof Bzip2Node) && stopNode.getParentNodes() != null)
				zipName = ((Node) stopNode.getParentNodes().get(stopNode.getParentNodes().size() - 1)).getName();
			String entryName = zipName;
			if (zipEntry != null)
				entryName = zipEntry.getName();
			else if (tarEntry != null)
				entryName = tarEntry.getName();
			else if (zipName.endsWith(".gz")) //$NON-NLS-1$
				entryName = zipName.substring(0, zipName.length() - 3);
			else if (zipName.endsWith(".bz2")) //$NON-NLS-1$
				entryName = zipName.substring(0, zipName.length() - 4);

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
					(zipEntry != null && zipEntry.isDirectory() || tarEntry != null && tarEntry.isDirectory());
			if (isFolder && storeFolders == null)
				storeFolders = Boolean.TRUE;
			if (node == null)
				node = root;
			Node existingNode = n == -1 ? null : node.getChildByName(names[n], false);
			if (existingNode != null) {
				existingNode.update(zipEntry != null ? (Object) zipEntry : tarEntry);
			} else {
				String name = n >= 0 ? names[n] : "/"; //$NON-NLS-1$
				Node newChild = zipEntry != null ? new ZipNode(this, zipEntry, name, isFolder)
						: tarEntry != null ? (Node) new TarNode(this, tarEntry, name, isFolder)
								: zipStream instanceof CBZip2InputStream ? (Node) new Bzip2Node(
										this, name, isFolder) : new GzipNode(this, name, isFolder);
				node.add(newChild, null);
				long entrySize = 0;
				ByteArrayOutputStream out = null;
				boolean isStopNode = stopNode != null && newChild.getFullPath().equals(stopNode.getFullPath());
				if ((zipPath == null || isNoEntry) && participant == null && (isStopNode || stopNode == null)) {
					byte[] buf = new byte[8000];
					try {
						for (int count = 0; (count = zipStream.read(buf)) != -1; ) {
							if (out == null)
								out = new ByteArrayOutputStream();
							out.write(buf, 0, count);
							if (isNoEntry)
								entrySize += count;
						}
					} catch (Exception e) {
						logError(e);
					}
					if (out != null) {
						byte[] content = out.toByteArray();
						newChild.setContent(content);
						if (stopNode != null)
							stopNode.setContent(content);
					}
				}
				if (participant != null) {
					participant.streamAvailable(zipStream, newChild);
				}
				if (zipStream instanceof ZipInputStream) {
					try {
						((ZipInputStream) zipStream).closeEntry();
					} catch (Exception e) {
						logError(e);
					}
				}
				newChild.setSize(zipEntry != null ? zipEntry.getSize()
						: tarEntry != null ? tarEntry.getSize() : entrySize);
				if (isStopNode)
					break;
			}
			state &= -1 ^ INIT_STARTED;
		}
	}

	public void setNodeContent(Node node, InputStream modelContent) {
		initialize(modelContent, null, node);
	}

	private InputStream detectStream(InputStream contents) throws IOException {
		BufferedInputStream in = new BufferedInputStream(contents);
		switch (type = detectType(in)) {
		default:
			return in;
		case ZIP:
			return new ZipInputStream(in);
		case TAR:
			return new TarInputStream(in);
		case GZ:
			return new GZIPInputStream(in);
		case TARGZ:
			return new TarInputStream(new GZIPInputStream(in));
		case BZ2:
			in.skip(2);
			return new CBZip2InputStream(in);
		case TARBZ2:
			in.skip(2);
			return new TarInputStream(new CBZip2InputStream(in));
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
			case TARBZ2:
				out.write(new byte[] { 'B', 'Z' });
				out = new TarOutputStream(new CBZip2OutputStream(out));
				break;
			case BZ2:
				out.write(new byte[] { 'B', 'Z' });
				out = new CBZip2OutputStream(out);
				break;
			}
			if (out instanceof TarOutputStream)
				((TarOutputStream) out).setLongFileMode(TarOutputStream.LONGFILE_GNU);
			saveNodes(out, root, type, isStoreFolders(), monitor);
//			setDirty(false);
		} catch (Exception e) {
			logError(e);
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
			TarEntry tarEntry = type == TAR || type == TARGZ || type == TARBZ2 ? new TarEntry(entryName) : null;
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

	public Node createFolderNode(Node parent, String name) {
		Node newNode = null;
		String[] names = splitName(name);
		for (int i = 0; i < names.length; i++) {
			newNode = parent.getChildByName(names[i], false);
			if (newNode == null) {
				newNode = parent.create(this, names[i], true);
				parent.add(newNode, null);
			}
			parent = newNode;
		}
		return newNode;
	}

	private String[] splitName(String name) {
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
		if (ZipEditorPlugin.DEBUG)
			System.out.println(zipPath + " disposed"); //$NON-NLS-1$
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

	public boolean isStoreFolders() {
		return storeFolders != null ? storeFolders.booleanValue() : ZipEditorPlugin.getDefault()
				.getPreferenceStore().getBoolean(PreferenceConstants.STORE_FOLDERS_IN_ARCHIVES);
	}

	public void setStoreFolders(boolean store) {
		storeFolders = store ? Boolean.TRUE : Boolean.FALSE;
	}
}
