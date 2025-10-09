/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum KEY_PROTOCOL {
	VALID((byte) 0x03);

	private byte code;

	private KEY_PROTOCOL(byte code) {
		this.code = code;
	}

	public static KEY_PROTOCOL getTypeByCode(byte code) {
		for (KEY_PROTOCOL e : KEY_PROTOCOL.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}

}
