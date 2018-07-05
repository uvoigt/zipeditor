package zipeditor.model;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class TarModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tar";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.TAR_FILE;
	}

}
