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

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

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
    
    public void setGroupId(int groupId) {
    	if (groupId == this.groupId)
    		return;
		this.groupId = groupId;
		model.setDirty(true);
		model.notifyListeners();
	}
    
    public void setGroupName(String groupName) {
    	if (groupName == this.groupName || groupName != null && groupName.equals(this.groupName))
    		return;
		this.groupName = groupName;
		model.setDirty(true);
		model.notifyListeners();
	}
    
    public void setUserId(int userId) {
    	if (userId == this.userId)
    		return;
		this.userId = userId;
		model.setDirty(true);
		model.notifyListeners();
	}
    
    public void setUserName(String userName) {
    	if (userName == this.userName || userName != null && userName.equals(this.userName))
    		return;
		this.userName = userName;
		model.setDirty(true);
		model.notifyListeners();
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
		case ZipModel.TARBZ2:
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
