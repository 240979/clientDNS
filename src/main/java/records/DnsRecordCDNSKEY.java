package records;

/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public class DnsRecordCDNSKEY extends DnsRecordDNSKEY {
    public DnsRecordCDNSKEY(byte[] rawMessage, int lenght, int startIndex) {
        super(rawMessage, lenght, startIndex);
    }
}
