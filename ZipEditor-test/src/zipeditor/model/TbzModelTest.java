package zipeditor.model;


public class TbzModelTest extends AbstractModelTest {

	@Override
	public String getArchiveName() {
		return "archive.tbz";
	}

	@Override
	public int getArchiveType() {
		return ZipModel.TARBZ2;
	}

}
