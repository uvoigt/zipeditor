/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import zipeditor.rpm.Rpm.Header;
import zipeditor.rpm.Rpm.Index;
import zipeditor.rpm.Rpm.Lead;

public class RpmOutputStream extends FilterOutputStream {

	private String fileName;
	private RpmVersion version;
	private byte type;
	private short os;
	private boolean initialized;

	public RpmOutputStream(OutputStream out, String fileName) {
		this(out, fileName, RpmVersion._3_0, true, (byte) 1);
	}

	public RpmOutputStream(OutputStream out, String fileName, RpmVersion version, boolean binary, byte os) {
		super(out);
		this.fileName = fileName;
		this.version = version;
		this.type = (byte) (binary ? 0 : 1);
		this.os = os;
	}

	public void putNextEntry(RpmEntry entry) throws IOException {
		if (!initialized) {
			writeLead();
			writeSignature();
			writeHeader();
			initialized = true;
		}
		
	}
	
	private void writeLead() throws IOException {
		Lead lead = new Lead(version.major, version.minor, type, (byte) 1, fileName.getBytes(), os, (byte) 5, new byte[6]);
		lead.write(out);
	}

	private void writeSignature() throws IOException {
		Index[] entries = new Index[5];
		entries[0] = new Index(62, 7, 0, 0, null); // TODO
		Header signature = new Header((byte) 1, entries);
		signature.write(out);
	}

	private void writeHeader() throws IOException {
		Index[] entries = new Index[5];
		entries[0] = new Index(62, 7, 0, 0, null); // TODO
		Header header = new Header((byte) 1, entries);
		header.write(out);
	}
}
