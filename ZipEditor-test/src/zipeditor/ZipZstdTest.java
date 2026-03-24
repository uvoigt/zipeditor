package zipeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.junit.Test;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdInputStreamNoFinalizer;

public class ZipZstdTest {

	@Test
	public void shouldThrow() {
		ZipException exception = assertThrows(ZipException.class, () -> new ZipFile("resources/invalid_zip64.zip"));
		assertEquals("Invalid CEN header (invalid zip64 extra data field size)", exception.getMessage());
	}

	@Test
	public void shouldNotThrow() throws Exception {
		try (ZipArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream("resources/invalid_zip64.zip"))) {
			ZipArchiveEntry entry = in.getNextEntry();
			assertEquals("test.txt", entry.getName());
			assertEquals(13, entry.getSize());
			assertEquals(13, entry.getCompressedSize());
			assertEquals("Hello, World!", new String(in.readAllBytes()));
		}
	}

	@Test
	public void shouldNotThrowZipInputStream() throws Exception {
		try (ZipInputStream in = new ZipInputStream(new FileInputStream("resources/invalid_zip64.zip"))) {
			ZipEntry entry = in.getNextEntry();
			assertEquals("test.txt", entry.getName());
			assertEquals(13, entry.getSize());
			assertEquals(13, entry.getCompressedSize());
			assertEquals("Hello, World!", new String(in.readAllBytes()));
		}
	}

	@Test
	public void shouldReadZstdWithFile() throws Exception {
		ZipException e = assertThrows(ZipException.class, () -> new ZipFile("resources/zstd.zip"));
		assertEquals("invalid CEN header (bad compression method: 93)", e.getMessage());
	}

	@Test
	public void shouldReadZstd() throws Exception {
		try (ZipInputStream in = new ZipInputStream(new FileInputStream("resources/zstd.zip"))) {
			assertEquals(".project", in.getNextEntry().getName());
			ZipException e = assertThrows(ZipException.class, () -> in.read(new byte[100]));
			assertEquals("invalid compression method", e.getMessage());
		}
	}

	@Test
	public void shouldReadZ() throws Exception {
		ZstdInputStream zin = new ZstdInputStream(new FileInputStream("resources/archive.zst"));
		byte[] bs = new byte[1];
		int c = zin.read();
		assertEquals('<', c);
		// assertEquals("<", new String(bs));
		zin.close();

		ZstdInputStream zin2 = new ZstdInputStream(new FileInputStream("resources/archive.zip"));
		IOException e = assertThrows(IOException.class, () -> zin2.read(bs));
		assertEquals("Unknown frame descriptor", e.getMessage());
		zin2.close();
	}

	public static OutputStream noClose(OutputStream out) {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				out.write(b, off, len);
			}

			@Override
			public void flush() throws IOException {
				out.flush();
			}

			@Override
			public void close() throws IOException {
				// do nothing
			}
		};

	}

	@Test
	public void doSave() throws Exception {
		try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new File("test-zstd.zip"))) {
			ZipArchiveEntry e1 = new ZipArchiveEntry("one.txt");
			e1.setMethod(ZipMethod.ZSTD.getCode());
			byte[] bytes = """
					text file
					with multiple line
					""".getBytes();
			e1.setSize(bytes.length);
			out.putArchiveEntry(e1);
			try (OutputStream zstdOutput = ZstdCompressorOutputStream.builder().setOutputStream(noClose(out)).get()) {
				zstdOutput.write(bytes);
				zstdOutput.flush();
			}
			out.closeArchiveEntry();
			ZipArchiveEntry e2 = new ZipArchiveEntry("two.txt");
			e2.setMethod(ZipMethod.ZSTD.getCode());
			String second = """
					another text file
					some more line
					""";
			e2.setSize(second.getBytes().length);
			out.putArchiveEntry(e2);
			try (OutputStream o2 = ZstdCompressorOutputStream.builder().setOutputStream(noClose(out)).get()) {
				o2.write(second.getBytes());
				o2.flush();
			}
			out.closeArchiveEntry();
		}

		boolean jni = true;
		org.apache.commons.compress.archivers.zip.ZipFile zipFile = org.apache.commons.compress.archivers.zip.ZipFile
				.builder().setZstdInputStreamFactory((inpStream) -> jni ? new ZstdInputStreamNoFinalizer(inpStream) : new io.airlift.compress.zstd.ZstdInputStream(inpStream))
				.setFile("test-zstd.zip").get();
		Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
		ZipArchiveEntry lastEntry = null;
		while (entries.hasMoreElements()) {
			ZipArchiveEntry entry = entries.nextElement();
			System.out.println(entry.getName() + " / " + entry.getSize() + " / " + entry.getCompressedSize());
			if (!entries.hasMoreElements())
				lastEntry = entry;
		}
		InputStream in = zipFile.getInputStream(lastEntry);
		System.out.println(new String(in.readAllBytes()));

		zipFile.close();
	}
}
