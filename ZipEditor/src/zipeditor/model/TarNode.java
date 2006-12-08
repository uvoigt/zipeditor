/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TarNode extends Node {
	private class EntryStream extends InputStream {
		private InputStream in;
		private EntryStream(TarEntry entry, TarInputStream in) throws IOException {
			for (TarEntry e = null; (e = in.getNextEntry()) != null; ) {
				if (!entry.equals(e))
					continue;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				in.copyEntryContents(out);
				this.in = new ByteArrayInputStream(out.toByteArray());
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
	private String groupName;
	private int userId;
	private String userName;
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
			return new EntryStream(tarEntry, model.getTarFile());
		return null;
	}
	
	public Node create(ZipModel model, String name, boolean isFolder) {
		return new TarNode(model, name, isFolder);
	}
}
