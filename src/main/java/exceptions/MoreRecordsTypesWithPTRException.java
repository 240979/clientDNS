/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

public class MoreRecordsTypesWithPTRException extends Exception {

	private static final long serialVersionUID = 1L;

	public MoreRecordsTypesWithPTRException() {
		super("It is not possible to ask qustion with PTR record and other type record");
	}
}
