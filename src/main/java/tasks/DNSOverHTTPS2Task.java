package tasks;

import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.RESPONSE_MDNS_TYPE;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import models.MessageParser;
import org.json.simple.parser.ParseException;
import tasks.runnables.RequestResultsUpdateRunnable;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Experimental class with usage of HTTP2 capable lib and wire format of requests
 */
public class DNSOverHTTPS2Task extends DNSTaskBase {
    private boolean isGet;

    public DNSOverHTTPS2Task(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, String resolverIP, NetworkInterface netInterface, boolean isGet) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface,null);
        this.isGet = isGet;
    }

    @Override
    protected void sendData() throws TimeoutException, MessageTooBigForUDPException, InterfaceDoesNotHaveIPAddressException, IOException, InterruptedException, ParseException, HttpCodeException, OtherHttpException, NotValidDomainNameException, NotValidIPException, URISyntaxException, NoSuchAlgorithmException {
        String msg = Base64.getUrlEncoder().encodeToString(messageAsBytes);
        // TODO fix sending via desired IP protocol
        //System.setProperty("java.net.preferIPv6Addresses","true");
        //System.setProperty("java.net.preferIPv6Stack","true");
        InetAddress[] addr = InetAddress.getAllByName("odvr.nic.cz");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://odvr.nic.cz/dns-query?dns="+msg))
                .version(HttpClient.Version.HTTP_2)
                .GET()
                .headers("Accept", "application/dns-message", "Accept-Encoding", "gzip, deflate, br", "User-Agent", "Client-DNS")
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        //System.setProperty("java.net.preferIPv6Addresses","true");
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .sslContext(SSLContext.getDefault())
                .build();
        //System.setProperty("java.net.preferIPv6Addresses","true");
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        setReceiveReply(response.body());


    }

    @Override
    protected void updateProgressUI() {

    }

    @Override
    protected void updateResultUI() {
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }
}
