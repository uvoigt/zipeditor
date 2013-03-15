package zipeditor.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

public class Bzip2Node extends Node {

	public Bzip2Node(ZipModel model, String name, boolean isFolder) {
		super(model, name, isFolder);
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new Bzip2Node(model, name, isFolder);
	}

	protected InputStream doGetContent() throws IOException {
		InputStream in = super.doGetContent();
		if (in != null) {
			return in;
		}
		InputStream fileInput = new FileInputStream(model.getZipPath());
		fileInput.skip(2);
		return new CBZip2InputStream(fileInput);
	}

}
