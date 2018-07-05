/*
 * (c) Copyright 2002, 2017 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import zipeditor.rpm.Rpm;

public class RpmRootNode extends RootNode {

	private Rpm rpm;

	public RpmRootNode(ZipModel model, Rpm rpm) {
		super(model);
		this.rpm = rpm;
	}

	public Rpm getRpm() {
		return rpm;
	}

	public Node create(ZipModel model, String name, boolean isFolder) {
		return new RpmNode(model, name, isFolder);
	}
}
