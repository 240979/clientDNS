/*
* Created by 240979
* */
package tasks;

import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.*;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import models.DoQClientInitializer;
import tasks.runnables.RequestResultsUpdateRunnable;

import javax.net.ssl.TrustManagerFactory;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DNSOverQUICTask  extends DNSTaskBase{
    private int resolverPort;
    private String resolverName;
    @Getter
    @Setter
    private boolean notFinished = true;

    public DNSOverQUICTask(boolean recursion, boolean adFlag, boolean cdFlag, boolean doFlag, String domain, Q_COUNT[] types, TRANSPORT_PROTOCOL transport_protocol, APPLICATION_PROTOCOL application_protocol, String resolverIP, String resolverName,  int resolverPort, NetworkInterface netInterface) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(recursion, adFlag, cdFlag, doFlag, domain, types, transport_protocol, application_protocol, resolverIP, netInterface, null);
        this.resolverPort = resolverPort;
        this.resolverName = resolverName;
    }
    @Override
    protected void sendData() throws KeyStoreException, NoSuchAlgorithmException, InterruptedException, IllegalArgumentException, CancellationException, ExecutionException, ChannelOutputShutdownException {
        setStartTime(System.nanoTime());
        try{
            OpenSsl.ensureAvailability();
        }
        catch (UnsatisfiedLinkError error) // Because missing library is type Error, not exception I had to catch it here
        {
            LOGGER.severe("Missing BoringSSL native!");
            throw new ExecutionException(error); // Recast it as exception so it can be caught in ui.GeneralController
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .applicationProtocols("doq")
                .trustManager(tmf) //tmf from JDK
                .keylog(true) //enable logging keys -- does not work?
                .build();
        // don't check anything
        QuicSslContext insecureContext = QuicSslContextBuilder.forClient()
                .applicationProtocols("doq")
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // don't check anything
                .keylog(true)
                .build();
        EventLoopGroup group = new NioEventLoopGroup();

        ChannelHandler codec = new QuicClientCodecBuilder()
                .sslContext(insecureContext)
                //.sslContext(context)
                .maxIdleTimeout(5, TimeUnit.SECONDS) // https://github.com/netty/netty-incubator-codec-quic/blob/main/codec-native-quic/src/test/java/io/netty/incubator/codec/quic/example/QuicClientExample.java
                .initialMaxData(65535)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .build();
        Bootstrap udpBootstrap = new Bootstrap();
        Channel channel = udpBootstrap
                .group(group)
                .channel(NioDatagramChannel.class)
                .handler(codec)
                .bind(0)
                .sync()
                .channel();
        QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                .streamHandler(new ChannelInboundHandlerAdapter())
                .remoteAddress(new InetSocketAddress(resolver, resolverPort)) //https://gist.github.com/leiless/df17252a17503d3ebf9a04e50f163114
                //.remoteAddress(new InetSocketAddress(resolverName, resolverPort))
                .connect()
                .get();
        ChannelInitializer<QuicStreamChannel> initializer = new DoQClientInitializer(this);
        QuicStreamChannel stream = quicChannel.createStream(QuicStreamType.BIDIRECTIONAL, initializer)
                .sync()
                .getNow();

        if (stream.isActive() && stream.isWritable()) {
            stream.writeAndFlush(Unpooled.wrappedBuffer(getMessageAsBytes())).sync();
            LOGGER.info("Message sent");
        } else {
            LOGGER.severe("QUIC stream inactive!");
            throw new ChannelOutputShutdownException("QUIC stream closed!");
        }

        setWasSend(true);
        //Thread.sleep(5000);
        while (notFinished) {
            Thread.sleep(10); // A lock would be better, but this works and I do not think, that it makes a difference
        }
        setStopTime(System.nanoTime());
        setDuration(calculateDuration());
        setMessagesSent(1);
        quicChannel.closeFuture();
        channel.close();
        group.shutdownGracefully();
    }

    @Override
    protected void updateProgressUI() {

    }

    @Override
    protected void updateResultUI() {
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }
}
