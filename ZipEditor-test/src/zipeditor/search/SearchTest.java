package zipeditor.search;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.search.ui.text.Match;
import org.junit.Assert;
import org.junit.Test;

public class SearchTest {

	@Test
	public void gzSearch() throws Exception {
		ZipSearchResult result = search("archive.gz");
		Assert(result, 1, 9);
	}

	@Test
	public void tarSearch() throws Exception {
		ZipSearchResult result = search("archive.tar");
		Assert(result, 2, 3);
	}

	@Test
	public void tarBz2Search() throws Exception {
		ZipSearchResult result = search("archive.tar.bz2");
		Assert(result, 2, 3);
	}

	@Test
	public void tarGzSearch() throws Exception {
		ZipSearchResult result = search("archive.tar.gz");
		Assert(result, 2, 3);
	}

	@Test
	public void tbzSearch() throws Exception {
		ZipSearchResult result = search("archive.tbz");
		Assert(result, 2, 3);
	}

	@Test
	public void tgzSearch() throws Exception {
		ZipSearchResult result = search("archive.tgz");
		Assert(result, 2, 3);
	}

	@Test
	public void zipSearch() throws Exception {
		ZipSearchResult result = search("archive.zip");
		Assert(result, 2, 3);
	}

	@Test
	public void largeSearch() throws Exception {
		File path = new File("resources/large.zip");
		List<File> files = new ArrayList<File>();
		files.add(path);

		ZipSearchOptions options = new ZipSearchOptions("", "Uwe Voigt", "Cp1252", true, ZipSearchOptions.SCOPE_WORKSPACE);
		ZipSearchQuery query = new ZipSearchQuery(options, files, null);
		IStatus status = query.run(new NullProgressMonitor());

		Assert.assertNotNull(status);
		Assert.assertEquals(IStatus.OK, status.getCode());
		ZipSearchResult result = (ZipSearchResult) query.getSearchResult();
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getMatchCount());
		Object[] elements = result.getElements();
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			int count = result.getMatchCount(element);
			Assert.assertEquals(1, count);
			Match[] matches = result.getMatches(element);
			Assert.assertEquals(8190, matches[0].getOffset());
			Assert.assertEquals(9, matches[0].getLength());
		}
	}

	@Test
	public void utf8Search() throws Exception {
		File path = new File("resources/utf8.zip");
		List<File> files = new ArrayList<File>();
		files.add(path);

		ZipSearchOptions options = new ZipSearchOptions("", new String(new byte[] {(byte) 0xe8, (byte)0xa1, (byte)0x8b}, "UTF8"), "UTF8", false, ZipSearchOptions.SCOPE_WORKSPACE);
		ZipSearchQuery query = new ZipSearchQuery(options, files, null);
		IStatus status = query.run(new NullProgressMonitor());

		Assert.assertNotNull(status);
		Assert.assertEquals(IStatus.OK, status.getCode());
		ZipSearchResult result = (ZipSearchResult) query.getSearchResult();
		Assert.assertEquals(1, result.getMatchCount());
		Object[] elements = result.getElements();
		Assert.assertEquals(1, elements.length);
		Object element = elements[0];
		Assert.assertEquals(1, result.getMatchCount(element));
		Match[] matches = result.getMatches(element);
		Assert.assertEquals(2123, matches[0].getOffset());
		Assert.assertEquals(1, matches[0].getLength());
	}

	protected ZipSearchResult search(String archiveName) {
		File path = new File("resources/" + archiveName);
		List<File> files = new ArrayList<File>();
		files.add(path);

		ZipSearchOptions options = new ZipSearchOptions("", "Uwe Voigt", "Cp1252", false, ZipSearchOptions.SCOPE_WORKSPACE);
		ZipSearchQuery query = new ZipSearchQuery(options, files, null);
		IStatus status = query.run(new NullProgressMonitor());

		Assert.assertNotNull(status);
		Assert.assertEquals(IStatus.OK, status.getCode());

		return (ZipSearchResult) query.getSearchResult();
	}

	protected void Assert(ZipSearchResult result, int expectedElements, int expectedMatchCount) {
		Assert.assertNotNull(result);
		Assert.assertEquals(expectedElements * expectedMatchCount, result.getMatchCount());
		Object[] elements = result.getElements();
		Assert.assertEquals(expectedElements, elements.length);

		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			int count = result.getMatchCount(element);
			Assert.assertEquals(expectedMatchCount, count);
			Match[] matches = result.getMatches(element);
			for (int j = 0; j < expectedMatchCount / 3; j++) {
				int index = 3 * j;
				int contentOffset = j * 1144;
				Assert.assertEquals(contentOffset + 223, matches[index].getOffset());
				Assert.assertEquals(9, matches[index].getLength());
				Assert.assertEquals(contentOffset + 975, matches[++index].getOffset());
				Assert.assertEquals(9, matches[index].getLength());
				Assert.assertEquals(contentOffset + 1037, matches[++index].getOffset());
				Assert.assertEquals(9, matches[index].getLength());
			}
		}
	}
}
