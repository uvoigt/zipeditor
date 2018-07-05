/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.rpm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RpmUtils {

	static void readFully(InputStream in, byte[] b) throws IOException {
		readFully(in, b, 0, b.length);
	}

	static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
		while (len > 0) {
			int n = in.read(b, off, len);
			if (n == -1)
				throw new EOFException();
			off += n;
			len -= n;
		}
	}

	static int read16(InputStream in) throws IOException {
		int result = in.read() << 8;
		result |= in.read();
		return result;
	}

	static int read32(InputStream in) throws IOException {
		int result = in.read() << 24;
		result |= in.read() << 16;
		result |= in.read() << 8;
		result |= in.read();
		return result;
	}

	static Number getNumber(byte[] buf, int offset, int length) {
		long result = 0;
		for (int i = 0, n = length - 8; n >= 0; i++, n -= 8) {
			result |= (buf[offset + i] & 0xff) << n;
		}
		return length > 32 ? new Long(result) : (Number) new Integer((int) result);
	}

	static void write16(OutputStream out, int value) throws IOException {
		out.write(value >> 8 & 0xff);
		out.write(value & 0xff);
	}

	static void write32(OutputStream out, int value) throws IOException {
		out.write(value >> 24 & 0xff);
		out.write(value >> 16 & 0xff);
		out.write(value >> 8 & 0xff);
		out.write(value & 0xff);
	}

	static String toHex(byte[] b) {
		return toHex(b, " "); //$NON-NLS-1$
	}

	static String toHex(byte[] b, String separator) {
		return toHex(b, 0, separator);
	}

	static String toHex(byte[] b, int wrapLength, String separator) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++) {
			if (i > 0 && separator != null)
				sb.append(separator);
			String s = Integer.toHexString(b[i] & 0xff);
			if (s.length() == 1)
				s = "0" + s; //$NON-NLS-1$
			sb.append(s);
			if (wrapLength > 0 && sb.length() % wrapLength == 0)
				sb.append('\n');
		}
		return sb.toString();
	}
}
