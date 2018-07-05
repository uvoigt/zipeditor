package zipeditor.model;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class ZipModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.zip";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.ZIP_FILE;
	}

}
