/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

public class TarNodeProperty extends NodeProperty {

	public final static int USER_ID = 20;
	public final static int USER_NAME = 21;
	public final static int GROUP_ID = 22;
	public final static int GROUP_NAME = 23;
	
	public final static TarNodeProperty PUSER_ID = new TarNodeProperty(USER_ID);
	public final static TarNodeProperty PUSER_NAME = new TarNodeProperty(USER_NAME);
	public final static TarNodeProperty PGROUP_ID = new TarNodeProperty(GROUP_ID);
	public final static TarNodeProperty PGROUP_NAME = new TarNodeProperty(GROUP_NAME);

	private TarNodeProperty(int type) {
		super(type);
	}
}
