package zipeditor.model;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public abstract class AbstractModelTest {

	protected ZipModel model;

	public abstract String getArchiveName();

	public abstract ContentTypeId getArchiveType();

	@Before
	public void before() throws Exception {
		File path = new File("resources/" + getArchiveName());
		InputStream inputStream = new FileInputStream(path);
		boolean readonly = false;

		model = new ZipModel(path, inputStream, readonly);
	}

	@Test
	public void shouldOpenNodes() throws Exception {
		assertEquals(getArchiveType(), model.getType());
		assertEquals("", model.getRoot().getPath());
		assertEquals(2, model.getRoot().getChildren().length);
		assertEquals("folder", model.getRoot().getChildByName("folder", false).getName());
		assertEquals("about.html", model.getRoot().getChildByName("about.html", false).getName());
		assertEquals("about.html", model.getRoot().getChildByName("folder", false) //
				.getChildByName("about.html", false).getName());
		assertEquals(null, model.getRoot().getChildByName("unknown", false));
	}

}
