/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.json.simple.JSONObject;

public class DnsRecordA extends DnsRecord {

	protected InetAddress ipAddress;
	protected String ipAddressAsString;
	protected String KEY_ADDRESS = "Ipv4";

	public DnsRecordA(byte[] rawMessage, int length, int startIndex) throws UnknownHostException {
		super(rawMessage, length, startIndex);
		parseRecord();
	}

	private void parseRecord() throws UnknownHostException {
		byte[] data = new byte[length];
		int j = 0;
		for (int i = startIndex; i < startIndex + length; i++) {
			data[j] = rawMessage[i];
			j++;
		}
		ipAddress = InetAddress.getByAddress(data);
		ipAddressAsString = ipAddress.getHostAddress();
	}

	@Override
	public String toString() {
		return KEY_ADDRESS + ": " + ipAddressAsString;
	}

	@Override
	public String getDataAsString() {

		return ipAddressAsString;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(KEY_ADDRESS, ipAddressAsString);
		return object;
	}

	@Override
	public String[] getValesForTreeItem() {
        return new String[]{ toString() };
	}

	@Override
	public String getDataForTreeViewName() {
		return ipAddressAsString;
	}

}
