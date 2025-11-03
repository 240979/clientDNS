/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package records;

import org.json.simple.JSONObject;

public class DnsRecord {
	protected int length;
	protected int startIndex;
	protected byte[] rawMessage;

	public DnsRecord(byte[] rawMessage, int length, int startIndex) {
		this.length = length;
		this.rawMessage = rawMessage;
		this.startIndex = startIndex;
	}

	public JSONObject getAsJson() {
		return new JSONObject();
	}

	public String[] getValesForTreeItem() {
		return null;
	}

	public String getStringToTreeView() {
		return null;
	}

	public int getLength() {
		return length;
	}

	public String getDataForTreeViewName() {
		return "";
	}

	public int getStartIndex() {
		return startIndex;
	}

	public byte[] getRawMessage() {
		return rawMessage;
	}

	// Has to be overided in the children
	public String getDataAsString() {
		return null;
	}

	protected byte[] get4bytes(int currentIndex) {
        return new byte[]{ rawMessage[currentIndex], rawMessage[currentIndex + 1], rawMessage[currentIndex + 2],
                rawMessage[currentIndex + 3] };
	}

}
