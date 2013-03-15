package zipeditor.model;


public class TarGzModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tar.gz";
	}

	@Override
	public int getArchiveType() {
		return ZipModel.TARGZ;
	}

}
