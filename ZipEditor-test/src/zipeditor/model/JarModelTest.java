package zipeditor.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class JarModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.jar";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.ZIP_FILE;
	}
	
	@Test
	@Override
	public void shouldOpenNodes() throws Exception {
		assertEquals(getArchiveType(), model.getType());
		RootNode rootNode = model.getRoot();
		assertEquals("", rootNode.getPath());
		assertEquals(2, rootNode.getChildren().length);
		// size information in entry is -1, but we have to get the correct size.
		assertEquals(3317, rootNode.getChildByName("MANIFEST.MF", true).getSize());
	}

}
