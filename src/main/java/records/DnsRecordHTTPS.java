/*
 * Created by 240979
 * Based on: https://github.com/xramos00/DNS_client
 *           https://github.com/mbio16/clientDNS
 */
package records;

public class DnsRecordHTTPS extends DnsRecordSVCB {

    public DnsRecordHTTPS(byte[] rawMessage, int length, int startIndex) {
        super(rawMessage, length, startIndex);
    }

}