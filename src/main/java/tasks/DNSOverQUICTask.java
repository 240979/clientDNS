/*
* Created by 240979
* */
package tasks;

import exceptions.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.OpenSsl;
import io.netty.incubator.codec.quic.*;
import javafx.application.Platform;
import lombok.Getter;
import models.ConnectionSettings;
import models.DoQClientInitializer;
import models.Ip;
import models.RequestSettings;
import tasks.runnables.RequestResultsUpdateRunnable;

import javax.net.ssl.TrustManagerFactory;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DNSOverQUICTask  extends DNSTaskBase{
    private final int resolverPort;
    private final boolean useResolverDomainName;
    private final String resolverDomainName;
    @Getter
    private final CountDownLatch latch = new CountDownLatch(1);

    public DNSOverQUICTask(RequestSettings rs, ConnectionSettings cs) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(rs, cs, null);
        this.resolverPort = cs.getResolverPort();
        this.useResolverDomainName = cs.isDomainNameUsed();
        this.resolverDomainName = cs.getResolverUri();
    }

    @Override
    protected void sendData() throws KeyStoreException, NoSuchAlgorithmException, InterruptedException, IllegalArgumentException, CancellationException, ExecutionException, ChannelOutputShutdownException, InterfaceDoesNotHaveIPAddressException, TimeoutException, java.util.concurrent.TimeoutException {
        setStartTime(System.nanoTime());
        try{
            OpenSsl.ensureAvailability();
        }
        catch (UnsatisfiedLinkError error) // Because missing library is type Error, not exception I had to catch it here
        {
            LOGGER.severe("Missing BoringSSL native!");
            throw new ExecutionException(error); // Recast it as exception so it can be caught in ui.GeneralController
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null); // null uses the default system trust store

        QuicSslContext context = QuicSslContextBuilder.forClient()
                .applicationProtocols("doq")
                .trustManager(tmf) //tmf from JDK
                .keylog(true) //enable logging keys
                .build();

        EventLoopGroup group = new NioEventLoopGroup();
        InetSocketAddress localAddress = new InetSocketAddress(Ip.getIpAddressFromInterface(interfaceToSend, resolver), 0);
        ChannelHandler codec = new QuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(5, TimeUnit.SECONDS) // https://github.com/netty/netty-incubator-codec-quic/blob/main/codec-native-quic/src/test/java/io/netty/incubator/codec/quic/example/QuicClientExample.java
                .initialMaxData(65535)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .build();
        Bootstrap udpBootstrap = new Bootstrap();
        Channel channel = udpBootstrap
                .group(group)
                .channel(NioDatagramChannel.class)
                .handler(codec)
                .bind(localAddress)
                .sync()
                .channel();
        String target = useResolverDomainName ? resolverDomainName : resolver;
        LOGGER.info("Target used: " + target);
        QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                .streamHandler(new ChannelInboundHandlerAdapter())
                .remoteAddress(new InetSocketAddress(target, resolverPort)) //https://gist.github.com/leiless/df17252a17503d3ebf9a04e50f163114
                .connect()
                .get();
        ChannelInitializer<QuicStreamChannel> initializer = new DoQClientInitializer(this);
        QuicStreamChannel stream = quicChannel.createStream(QuicStreamType.BIDIRECTIONAL, initializer)
                .sync()
                .getNow();

        if (stream.isActive() && stream.isWritable()) {
            stream.writeAndFlush(Unpooled.wrappedBuffer(getMessageAsBytes()))
                    .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            LOGGER.info("Message sent");
        } else {
            LOGGER.severe("QUIC stream inactive!");
            throw new ChannelOutputShutdownException("QUIC stream closed!");
        }

        setWasSend(true);
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        if (!completed || exc != null) {
            throw new TimeoutException();
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
