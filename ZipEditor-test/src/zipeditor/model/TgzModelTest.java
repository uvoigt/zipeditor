package zipeditor.model;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class TgzModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tgz";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.TGZ_FILE;
	}

}
