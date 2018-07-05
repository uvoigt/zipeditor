package zipeditor.rpm;

public class RpmVersion {

	public static final RpmVersion _3_0 = new RpmVersion((byte) 3, (byte) 0);

	final byte major;
	final byte minor;

	private RpmVersion(byte major, byte minor) {
		this.major = major;
		this.minor = minor;
	}
}
