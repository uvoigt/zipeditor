package zipeditor.model;


public class TgzModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tgz";
	}

	@Override
	public int getArchiveType() {
		return ZipModel.TARGZ;
	}

}
