/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;

import models.DomainConvert;

public class DnsRecordCNAME extends DnsRecord {

	protected String name;
	protected static String KEY_CNAME = "Name";

	public DnsRecordCNAME(byte[] rawMessage, int lenght, int startIndex) {
		super(rawMessage, lenght, startIndex);
		parseRecord();
	}

	private void parseRecord() {
		// decode MDNS becase DNS is ascii and MDSN is utf-8
		name = DomainConvert.decodeMDNS(rawMessage, startIndex);
	}

	@Override
	public String toString() {
		return KEY_CNAME + ": " + name;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(KEY_CNAME, name);
		return object;
	}

	@Override
	public String[] getValesForTreeItem() {
		String[] pole = { KEY_CNAME + ": " + name };
		return pole;
	}

	@Override
	public String getDataForTreeViewName() {
		return KEY_CNAME + " " + name;
	}

}
