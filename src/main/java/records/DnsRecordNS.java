/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;

public class DnsRecordNS extends DnsRecordCNAME {

	private static String KEY_NAMESERVER = "NameServer";

	public DnsRecordNS(byte[] rawMessage, int length, int startIndex) {
		super(rawMessage, length, startIndex);
	}

	@Override
	@SuppressWarnings("unchecked")
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(KEY_NAMESERVER, name);
		return object;
	}

	@Override
	public String[] getValesForTreeItem() {
        return new String[]{ KEY_NAMESERVER + ": " + name, };
	}

	@Override
	public String getDataForTreeViewName() {
		return name;
	}
}
