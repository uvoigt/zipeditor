/*
 * (c) Copyright 2002, 2023 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import zipeditor.model.IModelInitParticipant;
import zipeditor.model.ZipModel;
import zipeditor.model.ZipContentDescriber.ContentTypeId;

public class PlainModel extends ZipModel {

	public PlainModel(File path, InputStream inputStream) {
		super(path, inputStream);
	}


	public void init(IModelInitParticipant participant) {
		InputStream in = null;
		try {
			in = new FileInputStream(getZipPath());
			participant.streamAvailable(in, new PlainNode(this, getZipPath().getName()));
		} catch (FileNotFoundException e) {
			logError(e);
		} finally {
			if (getZipPath() != null && in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logError(e);
				}
			}
		}
	}

	public ContentTypeId getType() {
		return ContentTypeId.GZ_FILE;
	}
}
