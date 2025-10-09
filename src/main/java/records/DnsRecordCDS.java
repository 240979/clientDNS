package records;

/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
public class DnsRecordCDS extends DnsRecordDS {

    public DnsRecordCDS(byte[] rawMessage, int lenght, int startIndex) {
        super(rawMessage, lenght, startIndex);
    }
}
