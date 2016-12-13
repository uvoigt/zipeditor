/*
 * (c) Copyright 2002, 20015 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import java.io.InputStream;

public interface IModelInitParticipant {

	void streamAvailable(InputStream inputStream, Node node);
}
