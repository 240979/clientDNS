/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum DIGEST_TYPE {
	RESERVED((byte) 0x00), SHA1((byte) 0x01), SHA256((byte) 0x02);

	private byte code;

	private DIGEST_TYPE(byte code) {
		this.code = code;
	}

	public static DIGEST_TYPE getTypeByCode(byte code) {
		for (DIGEST_TYPE type : DIGEST_TYPE.values()) {
			if (type.code == code)
				return type;
		}
		return null;
	}
}
