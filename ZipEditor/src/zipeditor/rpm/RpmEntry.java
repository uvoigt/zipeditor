/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

public abstract class RpmEntry {

	public static RpmEntry create(String name) {
		return new RpmEntryAscii(name);
	}

	private String name;
	long fileSize;
	int mode;
	long mtime;

	protected RpmEntry(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return fileSize;
	}

	public boolean isDirectory() {
		return (mode & 0x004000) > 0;
	}

	public long getTime() {
		return mtime;
	}
}
