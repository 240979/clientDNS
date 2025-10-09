/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

public class NotValidDomainNameException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotValidDomainNameException() {
		super("Domain name is not in valid format");
	}

}
