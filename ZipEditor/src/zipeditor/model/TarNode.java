/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class TarNode extends Node {
	private class EntryStream extends InputStream {
		private InputStream in;
		private EntryStream(TarEntry entry, TarInputStream in) throws IOException {
			for (TarEntry e = null; (e = in.getNextEntry()) != null; ) {
				if (!entry.equals(e))
					continue;
				if (entry.getSize() < 10000000) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					in.copyEntryContents(out);
					this.in = new ByteArrayInputStream(out.toByteArray());
				} else {
					File tmpFile = new File(model.getTempDir(), Integer.toString((int) System.currentTimeMillis()));
					FileOutputStream out = new FileOutputStream(tmpFile);
					in.copyEntryContents(out);
					out.close();
					this.in = new FileInputStream(tmpFile);
				}
				break;
			}
			in.close();
		}
		public int read() throws IOException {
			return in != null ? in.read() : -1;
		}
		public void close() throws IOException {
			if (in != null)
				in.close();
		}
	};

	private TarEntry tarEntry;
	private int groupId;
	private String groupName = new String();
	private int userId;
	private String userName = new String();
	private int mode;

	public TarNode(ZipModel model, TarEntry entry, String name, boolean isFolder) {
		this(model, name, isFolder);
		tarEntry = entry;
		if (tarEntry != null) {
			size = tarEntry.getSize();
			if (tarEntry.getModTime() != null)
				time = tarEntry.getModTime().getTime();
			groupId = tarEntry.getGroupId();
			groupName = tarEntry.getGroupName();
			userId = tarEntry.getUserId();
			userName = tarEntry.getUserName();
			mode = tarEntry.getMode();
		}
	}

	public TarNode(ZipModel model, String name, boolean isFolder) {
		super(model, name, isFolder);
	}

    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public int getMode() {
        return mode;
    }

    protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		if (in != null)
			return in;
		if (tarEntry != null)
			return new EntryStream(tarEntry, getTarFile());
		return null;
	}
	
	private TarInputStream getTarFile() throws IOException {
		switch (model.getType()) {
		default:
		case ZipModel.TAR:
			return new TarInputStream(new FileInputStream(model.getZipPath()));
		case ZipModel.TARGZ:
			return new TarInputStream(new GZIPInputStream(new FileInputStream(model.getZipPath())));
		}
	}
	
	public void reset() {
		super.reset();
		size = tarEntry.getSize();
		if (tarEntry.getModTime() != null)
			time = tarEntry.getModTime().getTime();
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new TarNode(model, name, isFolder);
	}
}
