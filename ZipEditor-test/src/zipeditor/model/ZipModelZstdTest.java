package zipeditor.model;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.junit.Test;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

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
	public void extractZstdNode() throws IOException {
		ZipNode childByName = (ZipNode) model.getRoot().getChildByName(".project", false);
		assertEquals(".project", childByName.getName());
		assertEquals(childByName.getMethod(), ZipMethod.ZSTD.getCode());
		
		InputStream content = childByName.getContent();
		String string = new String(content.readAllBytes());

		String[] split = string.split("\n");
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", split[0]);
		assertEquals(23, split.length);
	}
	
}
