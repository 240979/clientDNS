/*
    Created by 240979.
    This was created using knowledge from DoQ module and with respect to:
    https://github.com/netty/netty-incubator-codec-http3/blob/main/src/test/java/io/netty/incubator/codec/http3/example/Http3ClientExample.java
*/
package tasks;

import exceptions.*;
import exceptions.TimeoutException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.OpenSsl;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import javafx.application.Platform;
import lombok.Getter;
import models.*;
import org.json.simple.parser.ParseException;

import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

public class DNSOverHTTPS3Task extends DNSOverHTTPSTask {

    @Getter
    private CountDownLatch latch = new CountDownLatch(1);


    public DNSOverHTTPS3Task(RequestSettings rs, ConnectionSettings cs)
            throws UnsupportedEncodingException, NotValidIPException,
            NotValidDomainNameException, UnknownHostException {
        super(rs, cs);
    }

    @Override
    protected void sendData() throws
            InterruptedException, ExecutionException, InterfaceDoesNotHaveIPAddressException,
            TimeoutException, IOException, ParseException,
            HttpCodeException, NotValidDomainNameException, NotValidIPException, NoSuchAlgorithmException, KeyStoreException {

        try {
            OpenSsl.ensureAvailability();
        } catch (UnsatisfiedLinkError error) {
            LOGGER.severe("Missing BoringSSL native!");
            throw new ExecutionException(error);
        }

        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);

        QuicSslContext sslContext = QuicSslContextBuilder.forClient()
                .applicationProtocols(Http3.supportedApplicationProtocols()) // "h3"
                .trustManager(tmf)
                .keylog(true)
                .build();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            InetSocketAddress localAddress = isDomainNameUsed
                    ? new InetSocketAddress(0)
                    : new InetSocketAddress(
                    Ip.getIpAddressFromInterface(interfaceToSend, resolver), 0);

            ChannelHandler codec = Http3.newQuicClientCodecBuilder()  // pre-configured for H3
                    .sslContext(sslContext)
                    .maxIdleTimeout(5, TimeUnit.SECONDS)
                    .initialMaxData(Http3.MIN_INITIAL_MAX_STREAM_DATA_UNIDIRECTIONAL)
                    .initialMaxStreamDataBidirectionalLocal(65535)
                    .initialMaxStreamDataBidirectionalRemote(65535)
                    .initialMaxStreamsBidirectional(100)
                    .initialMaxStreamsUnidirectional(Http3.MIN_INITIAL_MAX_STREAMS_UNIDIRECTIONAL)
                    .build();

            Channel channel = new Bootstrap()
                    .group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(localAddress)
                    .sync()
                    .channel();

            String target = isDomainNameUsed ? serverDomainName : resolver;
            LOGGER.info("DoH3 target: " + target + ":" + 443);

            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                    .handler(new Http3ClientConnectionHandler())
                    .remoteAddress(new InetSocketAddress(target, 443))
                    .connect()
                    .get();

            // Open H3 request stream with codec
            QuicStreamChannel stream = Http3.newRequestStreamBootstrap(quicChannel, new DoH3ClientInitializer(this))
                    .create()
                    .sync()
                    .getNow();

            // Build and send HEADERS frame
            String uri;
            if (isReqJsonFormat) {
                uri = addParamsToUriAsJson(target + "/" + path, new String[] {domainAsString, qcountAsString(), "" + doFlag, "" + cdFlag} );
            } else {
                uri = addParamsToUriAsBase64Url(target + "/" + path);
            }
            LOGGER.info("Uri used: " + uri);
            setUsedUri(uri);
            Http3Headers headers;
            String withoutScheme = uri.substring("https://".length());
            String host = withoutScheme.split("/")[0];
            String fullPath = withoutScheme.substring(host.length());
            if(isGet){
                LOGGER.info("Method used: GET");
                // GET and both JSON or WIRE
                headers = new DefaultHttp3Headers()
                        .method("GET")
                        .scheme("https")
                        .authority(host)
                        .path(fullPath)
                        .add("accept", isReqJsonFormat ? "application/dns-json" : "application/dns-message");
                setMessagesSent(1);
                startTime = System.nanoTime();

                stream.writeAndFlush(new DefaultHttp3HeadersFrame(headers))
                        .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            }else if(isReqJsonFormat){
                // POST and JSON means do it as GET JSON, but with POST -- req. in URL
                LOGGER.info("Method used: POST and JSON");
                headers = new DefaultHttp3Headers()
                        .method("POST")
                        .scheme("https")
                        .authority(host)
                        .path(fullPath)
                        .add("accept", "application/dns-json");
                setMessagesSent(1);
                startTime = System.nanoTime();

                stream.writeAndFlush(new DefaultHttp3HeadersFrame(headers))
                        .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            }else{
                // POST and WIRE
                LOGGER.info("Method used: POST and WIRE");
                fullPath = "/" + path;
                headers = new DefaultHttp3Headers()
                        .method("POST")
                        .scheme("https")
                        .authority(host)
                        .path(fullPath)
                        .add("accept", "application/dns-message")
                        .add("content-type", "application/dns-message");
                setMessagesSent(1);
                startTime = System.nanoTime();
                // Because I need to put the request in the body, I need to send HEADERS and then DATA
                stream.write(new DefaultHttp3HeadersFrame(headers));
                stream.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.wrappedBuffer(getMessageAsBytes())))
                        .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            }

            LOGGER.info("DoH3 request sent");
            setWasSend(true);

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            stopTime = System.nanoTime();
            setStartTime(startTime);
            setStopTime(stopTime);
            setDuration(calculateDuration());

            if (!completed || exc != null){
                if(exc instanceof HttpCodeException)
                    throw new HttpCodeException(((HttpCodeException) exc).getCode());
                throw new TimeoutException();
            }

            byteSizeResponseDoHDecompressed = receiveReply.length;
            responseCode = 200;

            stream.closeFuture();
            quicChannel.close().sync();
            channel.close().sync();

        } finally {
            group.shutdownGracefully();
            if (!massTesting) {
                Platform.runLater(() ->
                        controller.getSendButton().setText(controller.getButtonText()));
            }
        }
    }
    @Override
    protected MessageParser parseResponse() throws QueryIdNotMatchException, UnknownHostException, UnsupportedEncodingException {
        if (httpResponse != null) // JSON path
            return new MessageParser(httpResponse);
        return super.parseResponse(); // wire format path
    }
    public void resetLatch() {
        latch = new CountDownLatch(1);
    }
}