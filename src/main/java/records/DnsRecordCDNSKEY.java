package records;

/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public class DnsRecordCDNSKEY extends DnsRecordDNSKEY {
    public DnsRecordCDNSKEY(byte[] rawMessage, int length, int startIndex) {
        super(rawMessage, length, startIndex);
    }
}
