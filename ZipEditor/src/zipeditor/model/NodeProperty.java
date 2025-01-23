/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

import zipeditor.Messages;

public class NodeProperty {

	public static final int NAME = 1;
	public static final int TYPE = 2;
	public static final int DATE = 3;
	public static final int SIZE = 4;
	public static final int PATH = 9;
	public static final int PERSISTED = 12;
	public static final int METHOD = 13;
	public static final NodeProperty PNAME = new NodeProperty(NAME);
	public static final NodeProperty PTYPE = new NodeProperty(TYPE);
	public static final NodeProperty PDATE = new NodeProperty(DATE);
	public static final NodeProperty PSIZE = new NodeProperty(SIZE);
	public static final NodeProperty PPATH = new NodeProperty(PATH);
	public static final NodeProperty PPERSISTED = new NodeProperty(PERSISTED);
	public static final NodeProperty PMETHOD = new NodeProperty(METHOD);
	protected int type;

	protected NodeProperty(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public String toString() {
		return Messages.getString("ZipNodeProperty." + type); //$NON-NLS-1$
	}

}