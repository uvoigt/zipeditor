package zipeditor.model;


public class TarBz2ModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tar.bz2";
	}

	@Override
	public int getArchiveType() {
		return ZipModel.TARBZ2;
	}

}
