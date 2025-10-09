/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum RD {
	RECURSIVE((boolean) true), ITERATIVE((boolean) false);

	public boolean code;

	private RD(boolean code) {
		this.code = code;
	}

	public static RD getTypeByCode(boolean code) {
		for (RD e : RD.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}

}
