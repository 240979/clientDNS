/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package enums;

import models.UInt16;

public enum CACHE {

	NO_FLUSH_CACHE(false, 0), FLUSH_CACHE(true, 32768);

	public boolean code;
	public int value;

	private CACHE(boolean code, int value) {
		this.code = code;
		this.value = value;
	}

	public static CACHE getTypeByCode(UInt16 value) {
		if (value.getValue() >= FLUSH_CACHE.value) {
			return FLUSH_CACHE;
		} else {
			return NO_FLUSH_CACHE;
		}
	}
}
