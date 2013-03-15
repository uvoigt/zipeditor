package zipeditor.model;


public class TarModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tar";
	}

	@Override
	public int getArchiveType() {
		return ZipModel.TAR;
	}

}
