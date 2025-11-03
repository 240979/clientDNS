package records;

/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public class DnsRecordCDS extends DnsRecordDS {

    public DnsRecordCDS(byte[] rawMessage, int length, int startIndex) {
        super(rawMessage, length, startIndex);
    }
}
