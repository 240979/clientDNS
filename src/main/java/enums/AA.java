/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

public enum AA {
	AUTHORITATIVE((boolean) true), NON_AUTHORITATIVE((boolean) false);

	public boolean code;

	private AA(boolean code) {
		this.code = code;
	}

	public static AA getTypeByCode(boolean code) {
		for (AA e : AA.values()) {
			if (e.code == code)
				return e;
		}
		return null;
	}
}
