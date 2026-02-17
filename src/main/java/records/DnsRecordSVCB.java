package records;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;


public class DnsRecordSVCB extends DnsRecord{
    protected int priority;
    protected String targetName;
    protected String[] alpnList     = new String[0];
    protected int port              = -1;
    protected String ipv4hint       = null;
    protected String ipv6hint       = null;
    protected boolean noDefaultAlpn = false;
    protected String ech            = null; // ECH == Encrypted Client Hello

    private static final int PARAM_ALPN         = 1;
    private static final int PARAM_NO_DEF_ALPN  = 2;
    private static final int PARAM_PORT         = 3;
    private static final int PARAM_IPV4HINT     = 4;
    private static final int PARAM_ECH          = 5;
    private static final int PARAM_IPV6HINT     = 6;

    private static final String KEY_PRIORITY    = "Priority";
    private static final String KEY_TARGET      = "Target";
    private static final String KEY_ALPN        = "ALPN";
    private static final String KEY_PORT        = "Port";
    private static final String KEY_IPV4HINT    = "IPv4 Hint";
    private static final String KEY_IPV6HINT    = "IPv6 Hint";
    private static final String KEY_ECH         = "ECH";
    private static final String KEY_NO_DEF_ALPN = "No Default ALPN";
    public DnsRecordSVCB(byte[] rawMessage, int length, int startIndex) {
        super(rawMessage, length, startIndex);
        parseRecord();
    }
    protected void parseRecord() {
        int currentIndex = startIndex;
        int endIndex = startIndex + length;

        //SvcPriority
        priority = ((rawMessage[currentIndex] & 0xFF) << 8) | (rawMessage[currentIndex + 1] & 0xFF);
        currentIndex += 2;

        // TargetName, should end with 0x00
        StringBuilder sb = new StringBuilder();
        while (currentIndex < endIndex && rawMessage[currentIndex] != 0x00) {
            int labelLen = rawMessage[currentIndex] & 0xFF;
            currentIndex++;
            for (int i = 0; i < labelLen && currentIndex < endIndex; i++, currentIndex++) {
                sb.append((char) rawMessage[currentIndex]);
            }
            if (rawMessage[currentIndex] != 0x00) {
                sb.append(".");
            }
        }
        currentIndex++; // skip 0x00
        targetName = !sb.isEmpty() ? sb.toString() : ".";

        // AliasMode (priority == 0) has no SvcParams
        if (priority == 0) return;

        // Parse SvcParams: key (2 bytes) + length (2 bytes) and then value
        while (currentIndex + 4 <= endIndex) {
            int paramKey = ((rawMessage[currentIndex] & 0xFF) << 8) | (rawMessage[currentIndex + 1] & 0xFF);
            currentIndex += 2;
            int paramLength = ((rawMessage[currentIndex] & 0xFF) << 8) | (rawMessage[currentIndex + 1] & 0xFF);
            currentIndex += 2;

            parseParam(paramKey, currentIndex, paramLength);
            currentIndex += paramLength;
        }
    }
    private void parseParam(int key, int offset, int len) {
        int endOffset = offset + len;
        switch (key) {
            case PARAM_ALPN: {
                ArrayList<String> protocols = new ArrayList<>();
                int i = offset;
                while (i < endOffset) {
                    int protocolLength = rawMessage[i] & 0xFF;
                    i++;
                    protocols.add(new String(rawMessage, i, protocolLength));
                    i += protocolLength;
                }
                alpnList = protocols.toArray(new String[0]);
                break;
            }
            case PARAM_NO_DEF_ALPN:
                noDefaultAlpn = true;
                break;

            case PARAM_PORT:
                port = ((rawMessage[offset] & 0xFF) << 8) | (rawMessage[offset + 1] & 0xFF);
                break;

            case PARAM_IPV4HINT: {
                StringBuilder sb = new StringBuilder();
                for (int i = offset; i < endOffset; i += 4) {
                    if (i > offset) sb.append(", ");
                    sb.append(rawMessage[i]   & 0xFF).append(".")
                            .append(rawMessage[i+1] & 0xFF).append(".")
                            .append(rawMessage[i+2] & 0xFF).append(".")
                            .append(rawMessage[i+3] & 0xFF);
                }
                ipv4hint = sb.toString();
                break;
            }
            case PARAM_IPV6HINT: {
                StringBuilder sb = new StringBuilder();
                for (int i = offset; i < endOffset; i += 16) {
                    if (i > offset) sb.append(", ");
                    for (int j = 0; j < 16; j += 2) {
                        if (j > 0) sb.append(":");
                        sb.append(String.format("%02x%02x",
                                rawMessage[i+j] & 0xFF, rawMessage[i+j+1] & 0xFF));
                    }
                }
                ipv6hint = sb.toString();
                break;
            }
            case PARAM_ECH:
                ech = String.format("[%d bytes]", len);
                break;

            default:
                break;
        }
    }
    @Override
    public String getDataForTreeViewName() {
        if (priority == 0) {
            return "ALIAS -> " + targetName;
        }
        String alpn = alpnList.length > 0 ? String.join(",", alpnList) : "";
        return priority + " " + targetName + (alpn.isEmpty() ? "" : " [" + alpn + "]");
    }
    @Override
    public String[] getValesForTreeItem() {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();
        items.add(KEY_PRIORITY + ": " + priority);
        items.add(KEY_TARGET + ": " + targetName);
        if (alpnList.length > 0) {
            items.add(KEY_ALPN + ": " + String.join(", ", alpnList));
        }
        if (noDefaultAlpn) {
            items.add(KEY_NO_DEF_ALPN + ": true");
        }
        if (port != -1) {
            items.add(KEY_PORT + ": " + port);
        }
        if (ipv4hint != null) {
            items.add(KEY_IPV4HINT + ": " + ipv4hint);
        }
        if (ipv6hint != null) {
            items.add(KEY_IPV6HINT + ": " + ipv6hint);
        }
        if (ech != null) {
            items.add(KEY_ECH + ": " + ech);
        }
        return items.toArray(new String[0]);
    }
    @SuppressWarnings("unchecked")
    @Override
    public JSONObject getAsJson() {
        JSONObject object = new JSONObject();
        object.put(KEY_PRIORITY, priority);
        object.put(KEY_TARGET, targetName);
        if (alpnList.length > 0) {
            JSONArray alpnArray = new JSONArray();
            Collections.addAll(alpnArray, alpnList);
            object.put(KEY_ALPN, alpnArray);
        }
        if (noDefaultAlpn) {
            object.put(KEY_NO_DEF_ALPN, true);
        }
        if (port != -1) {
            object.put(KEY_PORT, port);
        }
        if (ipv4hint != null) {
            object.put(KEY_IPV4HINT, ipv4hint);
        }
        if (ipv6hint != null) {
            object.put(KEY_IPV6HINT, ipv6hint);
        }
        if (ech != null) {
            object.put(KEY_ECH, ech);
        }
        return object;
    }

    @Override
    public String getDataAsString() {
        return getDataForTreeViewName();
    }
}
