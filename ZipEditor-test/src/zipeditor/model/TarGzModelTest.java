package zipeditor.model;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class TarGzModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tar.gz";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.TGZ_FILE;
	}

}
