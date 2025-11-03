/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

public class DnsRecordPTR extends DnsRecordCNAME {

	public DnsRecordPTR(byte[] rawMessage, int length, int startIndex) {
		super(rawMessage, length, startIndex);
	}

}
