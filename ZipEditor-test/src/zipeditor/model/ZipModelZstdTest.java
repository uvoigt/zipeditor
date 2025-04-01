package zipeditor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.airlift.compress.zstd.ZstdInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.junit.Test;

import zipeditor.PreferenceConstants;
import zipeditor.ZipEditorPlugin;
import zipeditor.model.ZipContentDescriber.ContentTypeId;
import zipeditor.preferences.PreferenceUtils;

public class ZipModelZstdTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "zstd.zip";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.ZIP_FILE;
	}

	@Override
	public void shouldOpenNodes() throws Exception {
		assertEquals(getArchiveType(), model.getType());
		assertEquals("", model.getRoot().getPath());
		assertEquals(2, model.getRoot().getChildren().length);
		
		ZipNode childByName = (ZipNode) model.getRoot().getChildByName(".classpath", false);
		assertEquals(".classpath", childByName.getName());
		assertEquals(childByName.getMethod(), ZipMethod.ZSTD.getCode());
		
		childByName = (ZipNode) model.getRoot().getChildByName(".project", false);
		assertEquals(".project", childByName.getName());
		assertEquals(childByName.getMethod(), ZipMethod.ZSTD.getCode());
	}
	
	@Test
	public void extractZstdNode0() throws IOException {
		File zipPath = model.getZipPath();
		ZipFile zipFile = ZipFile.builder().setFile(zipPath).get();
		ZipArchiveEntry entry = zipFile.getEntry(".project");
		InputStream inputStream = zipFile.getInputStream(entry);
		assertTrue(inputStream instanceof ZstdCompressorInputStream);
		
		String string = new String(inputStream.readAllBytes());
		String[] split = string.split("\n");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", split[0]);
		assertEquals(23, split.length);
	}
	
	@Test
	public void extractZstdNode1() throws IOException {
		File zipPath = model.getZipPath();
		ZipFile zipFile = ZipFile.builder().setZstdInputStreamFactory(ZstdInputStream::new).setFile(zipPath).get();
		ZipArchiveEntry entry = zipFile.getEntry(".project");
		InputStream inputStream = zipFile.getInputStream(entry);
		assertTrue(inputStream instanceof ZstdInputStream);
		
		String string = new String(inputStream.readAllBytes());
		String[] split = string.split("\n");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", split[0]);
		assertEquals(23, split.length);

		assertTrue(PreferenceUtils.isAircompressorSelected());
		assertFalse(PreferenceUtils.isJNIZstdSelected());
		ZipEditorPlugin.getDefault().getPreferenceStore()
				.setValue(PreferenceConstants.PREFIX_EDITOR + PreferenceConstants.SELECTED_ZSTD_LIB, PreferenceUtils.JNI_LIBRARY);
		assertFalse(PreferenceUtils.isAircompressorSelected());
		assertTrue(PreferenceUtils.isJNIZstdSelected());
		
		zipPath = model.getZipPath();
		zipFile = ZipFile.builder().setFile(zipPath).get();
		entry = zipFile.getEntry(".project");
		inputStream = zipFile.getInputStream(entry);
		assertTrue(inputStream instanceof ZstdCompressorInputStream);

		string = new String(inputStream.readAllBytes());
		split = string.split("\n");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", split[0]);
		assertEquals(23, split.length);
	}
}
