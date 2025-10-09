/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package exceptions;

public class DnsServerIpIsNotValidException extends Exception {

	private static final long serialVersionUID = 1L;

	public DnsServerIpIsNotValidException() {
		// TODO Auto-generated constructor stub
		super("Ip address is not valid for DNS server");
	}

}
