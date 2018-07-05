/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

public class RpmInputStream extends FilterInputStream {

	private Rpm rpm;
	private RpmEntry current;
	private boolean closed;
	private boolean eof;
	private long entryBytesToRead;

	public RpmInputStream(InputStream in) throws IOException {
		super(in);

		rpm = new Rpm();
		rpm.init(in);

		this.in = new PushbackInputStream(new GZIPInputStream(in));
	}

	public RpmEntry getNextEntry() throws IOException {
		if (closed)
			throw new IOException();

		if (eof)
			return null;

		if (current != null)
			closeEntry();

		current = readCpio();
		if (current != null)
			entryBytesToRead = current.getSize();
		else
			eof = true;
		return current;
	}

	public Rpm getRpm() {
		return rpm;
	}

	public void closeEntry() throws IOException {
		if (current == null)
			return;

		long toSkip = entryBytesToRead;
		long skipped = 0;
		while (skipped < toSkip) {
			skipped += in.skip(toSkip - skipped);
		}

		int c = in.read();
		while (c == 0)
			c = in.read();
		((PushbackInputStream) in).unread(c);
		current = null;
	}

	private RpmEntry readCpio() throws IOException {
		byte[] buf = new byte[13 * 8 + 6];
		RpmUtils.readFully(in, buf, 0, 11 * 2);
		Number magic = RpmUtils.getNumber(buf, 0, 16);
		if (magic.intValue() == 0x71c7)
			return readCpio1(buf);
		else
			return readCpioAscii(buf);
	}

	private RpmEntry readCpio1(byte[] buf) {
		// TODO Auto-generated method stub
		return null;
	}

	private RpmEntry readCpioAscii(byte[] buf) throws IOException {
		RpmUtils.readFully(in, buf, 11 * 2, buf.length - 11 * 2);
//		System.out.println(new String(buf, 0, buf.length));
		// NEW ASCII
		String magic = new String(buf, 0, 6);
		if (magic.equals("070701")) { //$NON-NLS-1$
			int offset = 6;
			int len = 8;
			String ino = new String(buf, offset, len);
			String mode = new String(buf, offset += len, len);
			String uid = new String(buf, offset += len, len);
			String gid = new String(buf, offset += len, len);
			String nlink = new String(buf, offset += len, len);
			String mtime = new String(buf, offset += len, len);
			String fileSize = new String(buf, offset += len, len);

			String devMajor = new String(buf, offset += 8, 8);
			String devMinor = new String(buf, offset += 8, 8);
			String rdevMajor = new String(buf, offset += 8, 8);
			String rdevMinor = new String(buf, offset += 8, 8);

			String nameSize = new String(buf, offset += 8, 8);
			String check = new String(buf, offset += 8, 8);

			StringBuilder name = new StringBuilder();
			int c = in.read();
			while (c != 0) {
				name.append((char) c);
				c = in.read();
			}
			while (c == 0)
				c = in.read();
			if (c != -1)
				((PushbackInputStream) in).unread(c);
			String n = name.toString();
			if ("TRAILER!!!".equals(n)) //$NON-NLS-1$
				return null;
			RpmEntryAscii entry = new RpmEntryAscii(n);
			entry.magic = magic;
			entry.ino = ino;
			entry.mode = Integer.parseInt(mode, 16);
			entry.uid = uid;
			entry.gid = gid;
			entry.nlink = nlink;
			entry.mtime = Long.parseLong(mtime, 16) * 1000L;
			entry.fileSize = Long.parseLong(fileSize, 16);
			entry.devMajor = devMajor;
			entry.devMinor = devMinor;
			entry.rdevMajor = rdevMajor;
			entry.rdevMinor = rdevMinor;
			entry.nameSize = nameSize;
			entry.check = check;
			return entry;
		}
		throw new IOException();
	}

	public int read() throws IOException {
        byte[] oneBuf = new byte[1]; // TODO field
		int num = read(oneBuf, 0, 1);
        return num == -1 ? -1 : (oneBuf[0]) & 0xff;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (closed)
			throw new IOException();
		if (current == null)
			return -1;
		if (entryBytesToRead <= 0)
			return -1;
		int count = in.read(b, off, (int) Math.min(len, entryBytesToRead));
		entryBytesToRead -= count;
		return count;
	}

	public InputStream getInputStream(RpmEntry entry) throws IOException {
		if (current != null)
			throw new IllegalStateException();

		for (RpmEntry e = null; (e = getNextEntry()) != null; ) {
			if (entry.getName().equals(e.getName())) {
				entryBytesToRead = e.getSize();
				return this;
			}
		}
		throw new IOException();
	}
}
