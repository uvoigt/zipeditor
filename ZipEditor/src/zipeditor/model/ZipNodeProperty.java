package zipeditor.model;

import zipeditor.Messages;

public class ZipNodeProperty {
	
	public final static int NAME = 1;
	public final static int TYPE = 2;
	public final static int DATE = 3;
	public final static int SIZE = 4;
	public final static int RATIO = 5;
	public final static int PACKED_SIZE = 6;
	public final static int CRC = 7;
	public final static int ATTR = 8;
	public final static int PATH = 9;
	public final static int COMMENT = 10;
	
	public final static ZipNodeProperty PNAME = new ZipNodeProperty(NAME);
	public final static ZipNodeProperty PTYPE = new ZipNodeProperty(TYPE);
	public final static ZipNodeProperty PDATE = new ZipNodeProperty(DATE);
	public final static ZipNodeProperty PSIZE = new ZipNodeProperty(SIZE);
	public final static ZipNodeProperty PRATIO = new ZipNodeProperty(RATIO);
	public final static ZipNodeProperty PPACKED_SIZE = new ZipNodeProperty(PACKED_SIZE);
	public final static ZipNodeProperty PCRC = new ZipNodeProperty(CRC);
	public final static ZipNodeProperty PATTR = new ZipNodeProperty(ATTR);
	public final static ZipNodeProperty PPATH = new ZipNodeProperty(PATH);
	public final static ZipNodeProperty PCOMMENT = new ZipNodeProperty(COMMENT);

	private int type;

	private ZipNodeProperty(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	public String toString() {
		return Messages.getString("ZipNodeProperty." + type); //$NON-NLS-1$
	}
}
