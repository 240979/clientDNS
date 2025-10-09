/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;

public class DnsRecordNS extends DnsRecordCNAME {

	private static String KEY_NAMESERVER = "NameServer";

	public DnsRecordNS(byte[] rawMessage, int lenght, int startIndex) {
		super(rawMessage, lenght, startIndex);
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
		String[] pole = { KEY_NAMESERVER + ": " + name, };
		return pole;
	}

	@Override
	public String getDataForTreeViewName() {
		return name;
	}
}
