/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum OP_CODE {
	QUERY((byte) 0x00), RESPONSE((byte) 0x01), SERVER_STATUS((byte) 0x02);

	public byte code;

	private OP_CODE(Byte code) {
		this.code = code;
	}

	public static OP_CODE getTypeByCode(byte code) {
		for (OP_CODE e : OP_CODE.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}
}
