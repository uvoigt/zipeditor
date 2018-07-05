/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Rpm {
	static class Lead {

		private static final byte[] MAGIC = { (byte) 0xed, (byte) 0xab, (byte) 0xee, (byte) 0xdb };

		private byte[] magic = new byte[4];
		private byte major;
		private byte minor;
		private short type;
		private short archnum;
		private byte[] name = new byte[66];
		private short osnum;
		private short signature_type;
		private byte[] reserved = new byte[16];

		Lead(byte major, byte minor, short type, short archnum, byte[] name, short osnum,
				short signature_type, byte[] reserved) {
			this.major = major;
			this.minor = minor;
			this.type = type;
			this.archnum = archnum;
			System.arraycopy(name, 0, this.name, 0, Math.min(this.name.length, name.length));
			this.osnum = osnum;
			this.signature_type = signature_type;
			System.arraycopy(reserved, 0, this.reserved, 0, Math.min(this.reserved.length, reserved.length));
		}

		Lead(InputStream in) throws IOException {

			RpmUtils.readFully(in, magic);
			if (!Arrays.equals(magic, MAGIC))
				throw new IOException();

			major = (byte) in.read();
			minor = (byte) in.read();
			type = (short) RpmUtils.read16(in);
			archnum = (short) RpmUtils.read16(in);
			RpmUtils.readFully(in, name);
			osnum = (short) RpmUtils.read16(in);
			signature_type = (short) RpmUtils.read16(in);
			RpmUtils.readFully(in, reserved);
		}

		void write(OutputStream out) throws IOException {
			out.write(MAGIC);

			out.write(major);
			out.write(minor);
			RpmUtils.write16(out, type);
			RpmUtils.write16(out, archnum);
			out.write(name);
			RpmUtils.write16(out, osnum);
			RpmUtils.write16(out, signature_type);
			out.write(reserved);
		}

		public String toString() {
			return "Lead" //$NON-NLS-1$
					+ "\n magic: " + RpmUtils.toHex(magic) //$NON-NLS-1$
					+ "\n major: " + major //$NON-NLS-1$
					+ "\n minor: " + minor //$NON-NLS-1$
					+ "\n type: " + type //$NON-NLS-1$
					+ "\n archnum: " + archnum //$NON-NLS-1$
					+ "\n name: " + new String(name) //$NON-NLS-1$
					+ "\n osnum: " + osnum //$NON-NLS-1$
					+ "\n signature_type: " + signature_type //$NON-NLS-1$
					+ "\nreserved: " + RpmUtils.toHex(reserved); //$NON-NLS-1$
		}
	}

	static class Header {
		private class CopyInputStream extends FilterInputStream {

			private ByteArrayOutputStream bos;

			CopyInputStream(InputStream in) {
				super(in);
				bos = new ByteArrayOutputStream();
			}

			public int read() throws IOException {
				int c = super.read();
				bos.write(c);
				return c;
			}

			public int read(byte[] b, int off, int len) throws IOException {
				int c = super.read(b, off, len);
				bos.write(b, off, c);
				return c;
			}

			byte[] getCopy() {
				return bos.toByteArray();
			}
		}
		
		private static final byte[] MAGIC = { (byte) 0x8e, (byte) 0xad, (byte) 0xe8 };

		private byte[] magic = new byte[3];
		private byte version;
		private Index[] entries;
		private int length;

		private boolean validate = true;

		Header(byte version, Index[] entries) {
			this.version = version;
			this.entries = entries;
		}

		Header(InputStream in, boolean signature) throws IOException {

			if (signature && false) {
				byte[] buf = new byte[16];
				RpmUtils.readFully(in, buf);

				int offset = 0;
				System.arraycopy(buf, offset, magic, 0, offset += magic.length);
				if (!Arrays.equals(magic, MAGIC))
					throw new IOException();

				version = buf[offset++];
				offset += 4;

				int numEntries = RpmUtils.getNumber(buf, offset, 32).intValue();
				offset += 4;
				length = RpmUtils.getNumber(buf, offset, 32).intValue();

				int blength = buf.length;
				System.arraycopy(buf, 0, buf = new byte[blength + numEntries * 16 + length], 0, blength);
				RpmUtils.readFully(in, buf, blength, buf.length - blength);

				System.out.println(RpmUtils.toHex(buf));
			}
			if (signature)
				in = new CopyInputStream(in);

			RpmUtils.readFully(in, magic);
			if (!Arrays.equals(magic, MAGIC))
				throw new IOException();

			version = (byte) in.read();
			in.read();
			in.read();
			in.read();
			in.read();

			int numEntries = RpmUtils.read32(in);
			entries = new Index[numEntries];

			length = RpmUtils.read32(in);

			for (int i = 0; i < entries.length; i++) {
				Index index = new Index(in);
				entries[i] = index;
			}

			byte[] store = new byte[length];
			RpmUtils.readFully(in, store);

			for (int i = 0; i < entries.length; i++) {
				entries[i].setStore(store);
			}

			if (signature) {
				// 8-byte boundary
				while (length++ % 8 != 0)
					in.read();
			}
			if (validate & signature)
				System.out.println(RpmUtils.toHex(((CopyInputStream) in).getCopy()));

			if (validate && signature) {
				for (int i = 0; i < entries.length; i++) {
					Index entry = entries[i];
					if (entry.tag == 1004) {
						validateChecksum(RpmUtils.toHex((byte[]) entry.store, null), null, checksum(store, "MD5")); //$NON-NLS-1$
					}
					if (entry.tag == 269) {
						validateChecksum((String) entry.store, null, checksum(store, "SHA1")); //$NON-NLS-1$
					}
				}
			}
		}

		String checksum(byte[] store, String algo) throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(magic);
			out.write(version);
//			out.write(0);
//			out.write(0);
//			out.write(0);
//			out.write(0);
			RpmUtils.write32(out, entries.length);
			RpmUtils.write32(out, length - 5);
			for (int i = 0; i < entries.length; i++) {
				Index index = entries[i];
				if (index.tag == 269 || index.tag == 1004 || index.tag == 62)
					continue;//index = new Index(index.tag, index.type, index.offset, index.count, store);
				index.write(out);
			}
			out.write(store);
//			out.write(0);
//			out.write(0);
//			out.write(0);
//			out.write(0);
			
			System.out.println(RpmUtils.toHex(out.toByteArray()));
			MessageDigest sha1;
			try {
				sha1 = MessageDigest.getInstance(algo);
			} catch (NoSuchAlgorithmException e) {
				throw new IOException(e);
			}
			byte[] digest = sha1.digest(out.toByteArray());
			return RpmUtils.toHex(digest, null);
		}

		void write(OutputStream out) throws IOException {
			out.write(MAGIC);

			out.write(version);
			for (int i = 0; i < entries.length; i++) {
				entries[i].write(out);
			}
		}

		private void validateChecksum(String store, byte[] buf, String check) throws IOException {
//			try {
//				MessageDigest sha1 = MessageDigest.getInstance("SHA1"); //$NON-NLS-1$
//				byte[] next = new byte[buf.length- 4];
//				System.arraycopy(buf, 0, next, 0, next.length);
//				byte[] digest = sha1.digest(buf);
//				String hex = RpmUtils.toHex(digest).replace(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println(check);
				System.out.println(store);
				if (!check.equals(store))
					;//throw new IOException();
//			} catch (NoSuchAlgorithmException e) {
//				throw new IOException(e);
//			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < entries.length; i++) {
				entries[i].toString(sb, 1);
			}
			return "Header" //$NON-NLS-1$
					+ "\n magic: " + RpmUtils.toHex(magic) //$NON-NLS-1$
					+ "\n version: " + version //$NON-NLS-1$
					+ "\n entries: " //$NON-NLS-1$
					+ sb.toString();
		}
	}

	static class Index {
		private int tag;
		private int type;
		private int offset;
		private int count;
		private Object store;

		Index(int tag, int type, int offset, int count, Object store) {
			this.tag = tag;
			this.type = type;
			this.offset = offset;
			this.count = count;
			this.store = store;
		}

		Index(InputStream in) throws IOException {
			tag = RpmUtils.read32(in);
			type = RpmUtils.read32(in);
			offset = RpmUtils.read32(in);
			count = RpmUtils.read32(in);
		}

		void setStore(byte[] buf) throws IOException {
			switch (type) {
			case 2: // int8
				store = RpmUtils.getNumber(buf, offset, 8);
				break;
			case 3: // int16
				store = RpmUtils.getNumber(buf, offset, 16);
				break;
			case 4: // int32
				store = RpmUtils.getNumber(buf, offset, 32);
				break;
			case 5: // int64
				store = RpmUtils.getNumber(buf, offset, 64);
				break;
			case 6: // string
			case 9: // I18NSTRING_TYPE	
				store = getString(buf, offset);
				break;
			case 7: // bin
				store = new byte[count];
				System.arraycopy(buf, offset, store, 0, count);
				break;
			case 8: // string-array
				String[] strings = new String[count];
				for (int i = 0, j = offset; i < count; i++) {
					String s = getString(buf, j);
					strings[i] = s;
					j += s.length() + 1;
				}
				store = strings;
				break;
			default:
				throw new IOException();
			}
			
		}

		private String getString(byte[] buf, int offset) throws IOException {
			for (int i = offset; i < buf.length; i++) {
				if (buf[i] == 0) {
					return new String(buf, offset, i - offset);
				}
			}
			throw new IOException();
		}

		private void appendIndent(StringBuilder sb, int indent) {
			while (indent > 0) {
				sb.append("  "); //$NON-NLS-1$
				indent--;
			}
		}

		void toString(StringBuilder sb, int indent) {
			sb.append('\n');
			appendIndent(sb, indent);
			sb.append("Index\n"); //$NON-NLS-1$
			appendIndent(sb, indent);
			sb.append("tag: ").append(tag).append('\n'); //$NON-NLS-1$
			appendIndent(sb, indent);
			sb.append("type: ").append(type).append('\n'); //$NON-NLS-1$
			appendIndent(sb, indent);
			sb.append("offset: ").append(offset).append('\n'); //$NON-NLS-1$
			appendIndent(sb, indent);
			sb.append("count: ").append(count).append('\n'); //$NON-NLS-1$
			appendIndent(sb, indent);
			sb.append("store: "); //$NON-NLS-1$
			if (store instanceof String[])
				sb.append(Arrays.asList((String[]) store));
			else if (store instanceof byte[])
				sb.append(RpmUtils.toHex((byte[]) store));
			else
				sb.append(store);
		}

		void write(OutputStream out) throws IOException {
			RpmUtils.write32(out, tag);
			RpmUtils.write32(out, type);
			RpmUtils.write32(out, offset);
			RpmUtils.write32(out, count);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString(sb, 0);
			return sb.toString();
		}
	}

	private Lead lead;
	private Header signature;
	private Header header;

	private boolean debug = Boolean.getBoolean("RpmInputStream.debug"); //$NON-NLS-1$

	public Lead getLead() {
		return lead;
	}

	public void setLead(Lead lead) {
		this.lead = lead;
	}

	public Header getSignature() {
		return signature;
	}

	public void setSignature(Header signature) {
		this.signature = signature;
	}

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	public void init(InputStream in) throws IOException {
		lead = new Lead(in);
		if (debug)
			System.out.println(lead);

		signature = new Header(in, true);
		if (debug)
			System.out.println("Signature - " + signature); //$NON-NLS-1$

		header = new Header(in, false);
		if (debug)
			System.out.println("Header - " + header); //$NON-NLS-1$
	}
}
