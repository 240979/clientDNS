package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import models.Ip;
import models.MessageParser;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.apache.hc.core5.http.protocol.HttpContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import tasks.runnables.RequestResultsUpdateRunnable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

/**
 * Class representing protocol DNS over HTTPS
 */
public class DNSOverHTTPSTask extends DNSTaskBase {

    private final boolean cdFlag;
    private final boolean isGet;
    private String serverDomainName;
    public DNSOverHTTPSTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain,
                            Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                            APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface,
                            boolean isGet, String resolverUri)
            throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface,null);
        this.cdFlag = cdFlag;
        this.isGet = isGet;
        this.serverDomainName = resolverUri;
    }
    public DNSOverHTTPSTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain,
                            Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                            APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface,
                            boolean isGet)
            throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface, null);
        this.cdFlag = cdFlag;
        this.isGet = isGet;
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException, IOException, InterruptedException, ParseException, HttpCodeException, OtherHttpException, NotValidDomainNameException, NotValidIPException, QueryIdNotMatchException, ProtocolException, NoSuchAlgorithmException {
           String httpsDomain = resolver.split("/")[0];
            //String httpsDomain = serverDomainName.split("/")[0];
            CloseableHttpResponse response;
            String[] values = new String[]{domainAsString, qcountAsString(), "" + doFlag, "" + cdFlag};

            setMessagesSent(1);

            String uri = addParamToUri(resolver, httpRequestParamsName, values);
            //String uri = addParamToUris(serverDomainName, httpRequestParamsName, values);
            updateProgressUI();

            response = sendAndReceiveDoH(uri, httpsDomain, isGet);
            setDuration(calculateDuration());
            if (response.getCode() == 200) {
                try
                {
                    String content = EntityUtils.toString(response.getEntity());
                    JSONParser parser = new JSONParser();
                    this.httpResponse = (JSONObject) parser.parse(content);
                    byteSizeResponseDoHDecompresed = getAllHeadersSize(response.getHeaders());
                    byteSizeResponseDoHDecompresed += content.getBytes().length;
                    parseResponseDoh(content);
                }
                catch (org.apache.hc.core5.http.ParseException e)
                {
                    // Had to do this using try-catch, because this method already throws ParseException, but from JSON
                    throw new ProtocolException("Parsing error in response");
                }
            } else {
                throw new HttpCodeException(response.getCode());
            }
            closeHttpConnection();
            if(!massTesting){
                Platform.runLater(() -> controller.getSendButton().setText(controller.getButtonText()));
            }
    }

    @Override
    protected void updateProgressUI() {
    }

    @Override
    protected void updateResultUI() {
        setByteSizeResponse(byteSizeResponseDoHDecompresed);
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }

    @Override
    public void stopExecution() {
        closeHttpConnection();
    }

    @Override
    protected void cleanup() {
        closeHttpConnection();
    }

    @Override
    protected MessageParser parseResponse() throws QueryIdNotMatchException, UnknownHostException, UnsupportedEncodingException {
        return new MessageParser(httpResponse);
    }

    @Override
    protected void setRequestAndResponse(MessageParser parser) {
        setResponse(parseJSON("Answer", httpResponse));
        setRequest(getAsTreeItem());
    }

    @SuppressWarnings("unchecked")
    protected static TreeItem<String> parseJSON(String name, Object json) {
        TreeItem<String> item = new TreeItem<>();
        if (json instanceof JSONObject object) {
            item.setValue(name);
            ((Set<Map.Entry>) object.entrySet()).forEach(entry -> {
                String childName = (String) entry.getKey();
                Object childJson = entry.getValue();
                TreeItem<String> child = parseJSON(childName, childJson);
                item.getChildren().add(child);
            });
        } else if (json instanceof JSONArray) {
            item.setValue(name);
            JSONArray array = (JSONArray) json;
            for (int i = 0; i < array.size(); i++) {
                String childName = String.valueOf(i);
                Object childJson = array.get(i);
                TreeItem<String> child = parseJSON(childName, childJson);
                item.getChildren().add(child);
            }
        } else {
            item.setValue(name + " : " + json);
        }
        return item;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private String addParamToUri(String uri, String[] paramNames, String[] values) {

        String[] split = uri.split("/");
        if (Ip.isIpv6Address(split[0])) {

            uri = "[" + split[0] + "]";
            if (split.length > 1) {
                uri += "/" + split[1];
            }
        }
        //String result = "https://" + uri + "?";
        StringBuilder result = new StringBuilder("https://")
                .append(uri)
                .append("?");
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                result.append(paramNames[i])
                        .append("=")
                        .append(values[i]);
            } else {
                result.append("&")
                        .append(paramNames[i])
                        .append(values[i]);
            }
        }
        return result.toString();

    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    private String qcountAsString() {
        // String result = "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < qcountTypes.length; i++) {
            if (i == 0) {
                result.append(qcountTypes[i]);
            } else {
                result.append(",")
                        .append(qcountTypes[i]);
            }
        }
        return result.toString();
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified to use directly IP instead of domain name of DNS server
     * */
    private CloseableHttpResponse sendAndReceiveDoH(String uri, String host, boolean httpGet)
            throws ClientProtocolException, IOException, InterfaceDoesNotHaveIPAddressException, NoSuchAlgorithmException {
        HttpUriRequestBase request;
        if (httpGet) {
            request = new HttpGet(uri);

        } else {
            request = new HttpPost(uri);
        }
        request.addHeader("Accept", "application/dns-json");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("User-Agent", "Client-DNS");

        request.addHeader("Host", host);
        // host header omitted when using IP instead of domain
        if (!Ip.isIpValid(host)) {
            request.addHeader("Host", host);
        }

        InetAddress localAddr = null;
        try {
            if (Ip.isIpValid(host)) {
                localAddr = Ip.getIpAddressFromInterface(interfaceToSend, host);
            } else if (interfaceToSend != null && !interfaceToSend.getInterfaceAddresses().isEmpty()) {
                localAddr = interfaceToSend.getInterfaceAddresses().getFirst().getAddress();
            }
        } catch (Exception e) {
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
            throw new InterfaceDoesNotHaveIPAddressException();
        }

        //request.setConfig(getRequestConfig(host));
        httpRequestAsString(request);
        //httpClient = HttpClients.createDefault();
        httpClient = createHttpClientForIP(resolver.split("/")[0], serverDomainName, localAddr);
        startTime = System.nanoTime();
        CloseableHttpResponse response = httpClient.execute(request);
        stopTime = System.nanoTime();
        setStartTime(startTime);
        setStopTime(stopTime);
        return response;

    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    /*
    private RequestConfig getRequestConfig(String host) throws InterfaceDoesNotHaveIPAddressException {
        try {
            if (Ip.isIpValid(host)) {
                return RequestConfig.custom().setLocalAddress(Ip.getIpAddressFromInterface(interfaceToSend, host))
                        .build();
            }
            return RequestConfig.custom().setLocalAddress(interfaceToSend.getInterfaceAddresses().getFirst().getAddress())
                    .build();
        } catch (Exception e) {
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
            throw new InterfaceDoesNotHaveIPAddressException();
        }
    }
    */
    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private void httpRequestAsString(HttpUriRequestBase request) {
        StringBuilder result = new StringBuilder(request.toString());
        result.append("\n");
        for (Header httpHeader : request.getHeaders()) {
            result.append(httpHeader.toString())
                    .append("\n");
        }
        this.byteSizeQuery = result.toString().getBytes().length;
        setByteSizeQuery(byteSizeQuery);
        httpRequest = result.toString();
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private int getAllHeadersSize(Header[] allHeaders) {
        int size = 0;
        for (Header header : allHeaders) {
            size += header.toString().getBytes().length;
        }
        return size + 1;
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    protected void closeHttpConnection() {
        try {
            httpClient.close();
        } catch (Exception e) {
            // already closed, just log it
            LOGGER.info("Trying to close already closed connection");
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private void parseResponseDoh(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        this.httpResponse = (JSONObject) parser.parse(response);
    }
    // 240979: When accessing HTTPS using IP address, ssl will probably fail, so this is meant to check and create SSL to be used in HTTPS
    private CloseableHttpClient createHttpClientForIP(String ipAddress, String expectedHostname, InetAddress localAddress) throws NoSuchAlgorithmException {
        try {
            // https://docs.oracle.com/javase/7/docs/api/javax/net/ssl/HostnameVerifier.html
            HostnameVerifier hostnameVerifier = (hostname, session) -> {
                // IPv4 is plain, IPv6 can be surrounded by brackets "[::]"
                if (hostname.equals(ipAddress) || hostname.equals("[" + ipAddress + "]")) {
                    // Use the default verifier but with the expected hostname
                    return new DefaultHostnameVerifier().verify(expectedHostname, session);
                }
                // For other cases, use default verification
                return new DefaultHostnameVerifier().verify(hostname, session);
            };

            DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(
                    SSLContext.getDefault(),
                    hostnameVerifier
            );

            // Create connection manager with TLS strategy
            PoolingHttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create()
                            .setTlsSocketStrategy(tlsStrategy)
                            .build();

            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setRoutePlanner(new DefaultRoutePlanner(null) {
                        @Override
                        protected InetAddress determineLocalAddress(HttpHost firstHop, HttpContext context) {
                            return localAddress;
                        }
                    })
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Failed to create custom HTTP client: " + ExceptionUtils.getStackTrace(e));
            return HttpClients.createDefault();
        }
    }
}
