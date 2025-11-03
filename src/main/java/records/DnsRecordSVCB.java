package records;

public class DnsRecordSVCB extends DnsRecord{
    public DnsRecordSVCB(byte[] rawMessage, int length, int startIndex) {
        super(rawMessage, length, startIndex);
    }
}
