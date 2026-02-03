package tasks;

import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import models.Ip;
import models.LocalAddressRoutePlanner;
import models.MessageParser;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import tasks.runnables.RequestResultsUpdateRunnable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DNSOverHTTPSTask extends DNSTaskBase {

    private boolean cdFlag;
    private boolean isGet;
    private String serverDomainName;
    private CloseableHttpAsyncClient httpClient;
    private InetAddress localAddress;
    private boolean isReqJsonFormat;
    public DNSOverHTTPSTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain,
                            Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol,
                            APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface,
                            boolean isGet, String resolverUri, boolean isReqJsonFormat)
            throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface, null);
        this.cdFlag = cdFlag;
        this.isGet = isGet;
        this.serverDomainName = resolverUri;
        this.isReqJsonFormat = isReqJsonFormat;
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

    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException,
            IOException, InterruptedException, ParseException, HttpCodeException, OtherHttpException,
            NotValidDomainNameException, NotValidIPException, QueryIdNotMatchException, ExecutionException {

        String httpsDomain = resolver.split("/")[0];
        String[] values = new String[]{domainAsString, qcountAsString(), "" + doFlag, "" + cdFlag};

        setMessagesSent(1);
        String uri;
        if(isReqJsonFormat) {
            uri = addParamsToUriAsJson(resolver, httpRequestParamsName, values);
        } else {
            uri = addParamsToUriAsBase64Url(resolver, values);
        }
        updateProgressUI();

        SimpleHttpResponse response = sendAndReceiveDoH(uri, httpsDomain, isGet, isReqJsonFormat);
        setDuration(calculateDuration());

        if (response.getCode() == 200 && isReqJsonFormat) {
            String content = response.getBodyText();
            JSONParser parser = new JSONParser();
            this.httpResponse = (JSONObject) parser.parse(content);
            byteSizeResponseDoHDecompresed = getAllHeadersSize(response.getHeaders());
            byteSizeResponseDoHDecompresed += content.getBytes(StandardCharsets.UTF_8).length;
            parseResponseDoh(content);
        } else if (response.getCode() == 200 && !isReqJsonFormat) {
            // Server is probably waiting for Wire format
            setReceiveReply(response.getBodyBytes());
            byteSizeResponseDoHDecompresed = response.getBodyBytes().length;
        } else {
            throw new HttpCodeException(response.getCode());
        }

        closeHttpConnection();

        if (!massTesting) {
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
        if (httpResponse != null) // If this response is null => wire format was used => use basic byte parser, not json
            return new MessageParser(httpResponse);
        return super.parseResponse();

    }

    @Override
    protected void setRequestAndResponse(MessageParser parser) {
        if (httpResponse != null) {
            setResponse(parseJSON("Answer", httpResponse));
            setRequest(getAsTreeItem());
        }
        else
            super.setRequestAndResponse(parser);
    }

    @SuppressWarnings("unchecked")
    protected static TreeItem<String> parseJSON(String name, Object json) {
        TreeItem<String> item = new TreeItem<>();
        if (json instanceof JSONObject) {
            item.setValue(name);
            JSONObject object = (JSONObject) json;
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

    private String addParamsToUriAsJson(String uri, String[] paramNames, String[] values) {
        String[] split = uri.split("/");
        if (Ip.isIpv6Address(split[0])) {
            uri = "[" + split[0] + "]";
            if (split.length > 1) {
                uri += "/" + split[1];
            }
        }
        StringBuilder sb = new StringBuilder("https://")
                .append(uri)
                .append("?");
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                sb.append(paramNames[i])
                        .append("=")
                        .append(values[i]);
            } else {
                sb.append("&")
                        .append(paramNames[i])
                        .append("=")
                        .append(values[i]);
            }
        }
        return sb.toString();
    }
    private String addParamsToUriAsBase64Url(String uri, String[] values) {

        String[] split = uri.split("/");
        if (Ip.isIpv6Address(split[0])) {
            uri = "[" + split[0] + "]";
            if (split.length > 1) {
                uri += "/" + split[1];
            }
        }
        String query = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(getMessageAsBytes());

        return "https://" +
                uri +
                "?dns=" +
                query;
    }


    private String qcountAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < qcountTypes.length; i++) {
            if (i == 0) {
                sb.append(qcountTypes[i]);
            } else {
                sb.append(",").append(qcountTypes[i]);
            }
        }
        return sb.toString();
    }

    private SimpleHttpResponse sendAndReceiveDoH(String uri, String host, boolean httpGet, boolean isJson)
            throws IOException, InterfaceDoesNotHaveIPAddressException, InterruptedException, ExecutionException {
        LOGGER.info("URI used: " + uri);
        determineLocalAddress(host);

        SimpleHttpRequest request;
        if (httpGet) {
            request = SimpleRequestBuilder.get(uri).build();
        } else {
            request = SimpleRequestBuilder.post(uri).build();
        }

        if (isJson) request.addHeader("Accept", "application/dns-json");
        else request.addHeader("Accept", "application/dns-message");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("User-Agent", "Client-DNS");

        if (!Ip.isIpValid(host)) {
            request.addHeader("Host", host);
        }

        httpRequestAsString(request);

        H2Config h2Config = H2Config.custom()
                .setPushEnabled(false)
                .setMaxConcurrentStreams(100)
                .build();

        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(30))
                .build();

        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .build();

        httpClient = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setIOReactorConfig(ioReactorConfig)
                .setDefaultRequestConfig(getRequestConfig())
                .setH2Config(h2Config)
                .setRoutePlanner(new LocalAddressRoutePlanner(localAddress))
                .build();

        httpClient.start();


        CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();

        startTime = System.nanoTime();

        HttpContext context = new BasicHttpContext();
        if (localAddress != null) {
            context.setAttribute("http.local-address", new InetSocketAddress(localAddress, 0));
        }

        httpClient.execute(request, context, new FutureCallback<SimpleHttpResponse>() {
            @Override
            public void completed(SimpleHttpResponse result) {
                stopTime = System.nanoTime();
                setStartTime(startTime);
                setStopTime(stopTime);

                LOGGER.info("Using HTTP version: " + result.getVersion());

                future.complete(result);
            }

            @Override
            public void failed(Exception ex) {
                stopTime = System.nanoTime();
                setStartTime(startTime);
                setStopTime(stopTime);
                future.completeExceptionally(ex);
            }

            @Override
            public void cancelled() {
                stopTime = System.nanoTime();
                setStartTime(startTime);
                setStopTime(stopTime);
                future.completeExceptionally(new InterruptedException("Request cancelled"));
            }
        });

        return future.get();
    }

    private void determineLocalAddress(String host) throws InterfaceDoesNotHaveIPAddressException {
        try {
            if (Ip.isIpValid(host)) {
                localAddress = Ip.getIpAddressFromInterface(interfaceToSend, host);
            } else {
                localAddress = interfaceToSend.getInterfaceAddresses().getFirst().getAddress();
            }
        } catch (Exception e) {
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
            throw new InterfaceDoesNotHaveIPAddressException();
        }
    }

    private RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(30))
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .build();
    }

    private void httpRequestAsString(SimpleHttpRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod())
                .append(" ")
                .append(request.getRequestUri())
                .append("\n");

        for (Header header : request.getHeaders()) {
            sb.append(header.getName())
                    .append(": ")
                    .append(header.getValue())
                    .append("\n");
        }

        this.byteSizeQuery = sb.toString().getBytes(StandardCharsets.UTF_8).length;
        setByteSizeQuery(byteSizeQuery);
        httpRequest = sb.toString();
    }

    private int getAllHeadersSize(Header[] allHeaders) {
        int size = 0;
        for (Header header : allHeaders) {
            size += (header.getName() + ": " + header.getValue()).getBytes(StandardCharsets.UTF_8).length;
        }
        return size + 1;
    }

    protected void closeHttpConnection() {
        try {
            if (httpClient != null) {
                httpClient.close(org.apache.hc.core5.io.CloseMode.GRACEFUL);
            }
        } catch (Exception e) {
            LOGGER.info("Trying to close already closed connection");
        }
    }

    private void parseResponseDoh(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        this.httpResponse = (JSONObject) parser.parse(response);
    }
}