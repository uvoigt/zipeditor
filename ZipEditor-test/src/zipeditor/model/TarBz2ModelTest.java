package zipeditor.model;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class TarBz2ModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tar.bz2";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.TBZ_FILE;
	}

}
