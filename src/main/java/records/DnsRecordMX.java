/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;

import models.DomainConvert;
import models.UInt16;

public class DnsRecordMX extends DnsRecord {

	private UInt16 priority;
	private String nameServer;
	private static final String KEY_PRIORITY = "Priority";
	private static final String KEY_MAIL_EXCHANGE = "Mail exchange";

	// private static final String KEY_SERV
	public DnsRecordMX(byte[] rawMessage, int lenght, int startIndex) {
		super(rawMessage, lenght, startIndex);
		parse();
	}

	public void parse() {
		priority = new UInt16().loadFromBytes(rawMessage[startIndex], rawMessage[startIndex + 1]);

		startIndex += 2;
		nameServer = DomainConvert.decodeDNS(rawMessage, startIndex);
	}

	@Override
	public String getDataAsString() {

		return ("" + priority.getValue() + "	" + nameServer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getAsJson() {
		JSONObject object = new JSONObject();
		object.put(KEY_PRIORITY, priority.getValue());
		object.put(KEY_MAIL_EXCHANGE, nameServer);
		return object;
	}

	@Override
	public String[] getValesForTreeItem() {
		String[] pole = { KEY_PRIORITY + ": " + priority.getValue(), KEY_MAIL_EXCHANGE + ": " + nameServer };
		return pole;
	}

	@Override
	public String getDataForTreeViewName() {
		return priority.getValue() + " " + nameServer;
	}

}
