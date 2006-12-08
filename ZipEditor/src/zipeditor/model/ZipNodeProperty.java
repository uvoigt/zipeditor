/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package zipeditor.model;

public class ZipNodeProperty extends NodeProperty {
	
	public final static int RATIO = 5;
	public final static int PACKED_SIZE = 6;
	public final static int CRC = 7;
	public final static int ATTR = 8;
	public final static int COMMENT = 10;
	
	public final static ZipNodeProperty PRATIO = new ZipNodeProperty(RATIO);
	public final static ZipNodeProperty PPACKED_SIZE = new ZipNodeProperty(PACKED_SIZE);
	public final static ZipNodeProperty PCRC = new ZipNodeProperty(CRC);
	public final static ZipNodeProperty PATTR = new ZipNodeProperty(ATTR);
	public final static ZipNodeProperty PCOMMENT = new ZipNodeProperty(COMMENT);

	private ZipNodeProperty(int type) {
		super(type);
	}
}
