/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

public class RpmEntryNumeric extends RpmEntry {

	short magic;
	short dev;
	short ino;
	short mode;
	short uid;
	short gid;
	short nlink;
	short rdev;
	short[] mtime;
	short nameSize;
	short[] fileSize;

	RpmEntryNumeric(String name) {
		super(name);
	}
}
