/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import java.net.UnknownHostException;

import org.json.simple.JSONObject;

public class DnsRecordAAAA extends DnsRecordA {

	public DnsRecordAAAA(byte[] rawMessage, int length, int startIndex) throws UnknownHostException {
		super(rawMessage, length, startIndex);
		KEY_ADDRESS = "Ipv6";
	}

	@Override
	@SuppressWarnings("unchecked")
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(KEY_ADDRESS, ipAddressAsString);
		return object;
	}

}
