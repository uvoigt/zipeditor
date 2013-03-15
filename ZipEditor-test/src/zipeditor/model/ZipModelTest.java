package zipeditor.model;


public class ZipModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.zip";
	}

	@Override
	public int getArchiveType() {
		return ZipModel.ZIP;
	}

}
