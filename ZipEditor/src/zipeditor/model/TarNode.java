/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class TarNode extends Node {
	private class EntryStream extends FilterInputStream {
		private boolean close;
		private EntryStream(TarInputStream in) {
			super(in);
		}
		private EntryStream(TarEntry entry, TarInputStream in) throws IOException {
			super(in);
			close = true;
			for (TarEntry e = null; (e = in.getNextEntry()) != null; ) {
				if (!entry.equals(e))
					continue;
				break;
			}
		}
		public void close() throws IOException {
			if (close)
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

	public Object accept(NodeVisitor visitor, Object argument) throws IOException {
		TarRootNode rootNode = (TarRootNode) model.getRoot();
		TarInputStream tarStream = rootNode.getInputStream();
		if (tarStream != null && tarEntry != null) {
			TarEntry entry = tarStream.getNextEntry();
			if (!tarEntry.equals(entry)) {
				// when something has been added to or removed from the node tree
				do
					entry = tarStream.getNextEntry();
				while (entry != null && !tarEntry.equals(entry));
			}
		}
		return super.accept(visitor, argument);
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

    public String getModeString() {
    	StringBuilder sb = new StringBuilder(9);
		String s = "rwx"; //$NON-NLS-1$
		int mask = 0x100;
		for (int i = 0; i < 9; i++, mask >>= 1) {
			sb.append((mode & mask) > 0 ? s.charAt(i % 3) : '-');
		}
    	return sb.toString();
    }

    public void setGroupId(int groupId) {
    	if (groupId == this.groupId)
    		return;
		this.groupId = groupId;
		setModified(true);
	}
    
    public void setGroupName(String groupName) {
    	if (groupName == this.groupName || groupName != null && groupName.equals(this.groupName))
    		return;
		this.groupName = groupName;
		setModified(true);
	}
    
    public void setUserId(int userId) {
    	if (userId == this.userId)
    		return;
		this.userId = userId;
		setModified(true);
	}
    
    public void setUserName(String userName) {
    	if (userName == this.userName || userName != null && userName.equals(this.userName))
    		return;
		this.userName = userName;
		setModified(true);
	}

    public void setMode(int mode) {
    	if (mode != this.mode)
    		this.mode = mode;
	}

    public void setModeString(String modeString) {
    	if (modeString == null || modeString.length() != 9)
    		throw new IllegalArgumentException();
		int mode = 0;
		String s = "rwx"; //$NON-NLS-1$
		int mask = 0x100;
		for (int i = 0; i < 9; i++, mask >>= 1) {
			char c = modeString.charAt(i);
			if (c == s.charAt(i % 3)) {
				mode |= mask;
			} else if (c != '-')
				throw new IllegalArgumentException();
		}
		if ((mode & 0xfff) != (this.mode & 0xfff)) {
			this.mode = (this.mode & 0xff000) | mode;
			setModified(true);
		}
	}

    protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		if (in != null)
			return in;
		if (tarEntry != null) {
			TarRootNode rootNode = (TarRootNode) model.getRoot();
			TarInputStream tarStream = rootNode.getInputStream();
			if (tarStream != null)
				return new EntryStream(tarStream);
			if (model.getZipPath() != null)
				return new EntryStream(tarEntry, getTarFile(model));
		}
		return null;
	}

	static TarInputStream getTarFile(ZipModel model) throws IOException {
		switch (model.getType().getOrdinal()) {
		default:
		case ContentTypeId.TAR:
			return new TarInputStream(new FileInputStream(model.getZipPath()));
		case ContentTypeId.TGZ:
			return new TarInputStream(new GZIPInputStream(new FileInputStream(model.getZipPath())));
		case ContentTypeId.TBZ:
				InputStream in = new FileInputStream(model.getZipPath());
				in.skip(2);
				return new TarInputStream(new CBZip2InputStream(in));
		}
	}

	public void reset() {
		super.reset();
		size = tarEntry.getSize();
		if (tarEntry.getModTime() != null)
			time = tarEntry.getModTime().getTime();
	}
	
	public void update(Object entry) {
		if (!(entry instanceof TarEntry))
			return;
		TarEntry tarEntry = (TarEntry) entry;
		time = tarEntry.getModTime().getTime();
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new TarNode(model, name, isFolder);
	}
}
