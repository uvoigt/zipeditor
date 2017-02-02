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
	public void simpleSearch() throws Exception {
		File path = new File("resources/archive.zip");
		List<File> files = new ArrayList<File>();
		files.add(path);

		ZipSearchOptions options = new ZipSearchOptions("", "Uwe Voigt", "Cp1252", false, ZipSearchOptions.SCOPE_WORKSPACE);
		ZipSearchQuery query = new ZipSearchQuery(options, files, null);
		IStatus status = query.run(new NullProgressMonitor());

		Assert.assertNotNull(status);
		Assert.assertEquals(IStatus.OK, status.getCode());
		ZipSearchResult result = (ZipSearchResult) query.getSearchResult();
		Assert.assertNotNull(result);
		Assert.assertEquals(6, result.getMatchCount());
		Object[] elements = result.getElements();
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			int count = result.getMatchCount(element);
			Assert.assertEquals(3, count);
			Match[] matches = result.getMatches(element);
			Assert.assertEquals(223, matches[0].getOffset());
			Assert.assertEquals(9, matches[0].getLength());
			Assert.assertEquals(975, matches[1].getOffset());
			Assert.assertEquals(9, matches[1].getLength());
			Assert.assertEquals(1037, matches[2].getOffset());
			Assert.assertEquals(9, matches[2].getLength());
		}
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
}
