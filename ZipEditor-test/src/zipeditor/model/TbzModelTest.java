package zipeditor.model;

import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class TbzModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tbz";
	}

	@Override
	public ContentTypeId getArchiveType() {
		return ContentTypeId.TBZ_FILE;
	}

}
