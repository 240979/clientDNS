/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum KEY_USAGE {
	KEY_SIGNING_KEY((byte) 0x01), NOT_KEY_SIGNING_KEY((byte) 0x00);

	public byte code;

	private KEY_USAGE(byte code) {
		this.code = code;
	}

	public static KEY_USAGE getTypeByCode(byte code) {
		for (KEY_USAGE e : KEY_USAGE.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}
}
