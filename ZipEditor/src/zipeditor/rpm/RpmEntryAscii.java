/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

public class RpmEntryAscii extends RpmEntry {

	String magic;
	String ino;
	String uid;
	String gid;
	String nlink;
	String devMajor;
	String devMinor;
	String rdevMajor;
	String rdevMinor;
	String nameSize;
	String check;

	RpmEntryAscii(String name) {
		super(name);
	}
}
