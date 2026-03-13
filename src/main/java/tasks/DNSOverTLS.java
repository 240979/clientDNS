package tasks;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import exceptions.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import models.ConnectionSettings;
import models.DoTClientInitializer;
import models.Ip;
import models.RequestSettings;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.runnables.RequestResultsUpdateRunnable;

import javax.net.ssl.SSLException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Class representing protocol DNS over TLS
 */
@Getter
@Setter
public class DNSOverTLS extends DNSTaskBase{

    private SslContext sslCtx;
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private Channel channel;
    private volatile Exception exc = null;
    private boolean useResolverDomainName;
    private String resolverDomainName;
    private volatile CountDownLatch latch;

    public DNSOverTLS(RequestSettings rs, ConnectionSettings cs) throws UnsupportedEncodingException, NotValidIPException, NotValidDomainNameException, UnknownHostException {
        super(rs, cs, null);
        this.useResolverDomainName = cs.isDomainNameUsed();
        this.resolverDomainName = cs.getResolverUri();
    }
    protected void initConnection(String target) throws SSLException, InterruptedException, InterfaceDoesNotHaveIPAddressException {
        OpenSsl.ensureAvailability();
        LOGGER.info("Target used: " + target);
        sslCtx = SslContextBuilder.forClient()
                .protocols("TLSv1.3")
                .ciphers(Arrays.asList(
                        "TLS_AES_256_GCM_SHA384",
                        "TLS_AES_128_GCM_SHA256",
                        "TLS_CHACHA20_POLY1305_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"))
                .sslProvider(SslProvider.OPENSSL)
                .sessionTimeout(3000)
                .build();
        if(Epoll.isAvailable()){
            group = new EpollEventLoopGroup();
        } else{
            group = new NioEventLoopGroup();
        }

        bootstrap = new Bootstrap()
                .group(group)
                .channelFactory(() -> {
                    if (Epoll.isAvailable()) {
                        return new EpollSocketChannel();
                    } else {
                        return new NioSocketChannel();
                    }
                })
                .handler(new DoTClientInitializer(sslCtx, target, this));
        if(!useResolverDomainName){
            InetSocketAddress localAddress = new InetSocketAddress(Ip.getIpAddressFromInterface(interfaceToSend, resolver), 0);
            bootstrap.localAddress(localAddress);
        }
        channel = bootstrap.connect(target, 853).sync().channel();
        channel.config().setConnectTimeoutMillis(3000);
    }

    protected void sendSingleRequest() throws TimeoutException, InterruptedException {
        latch = new CountDownLatch(1);  // reset for each request
        exc = null;                      // reset exception
        channel.writeAndFlush(Unpooled.wrappedBuffer(getMessageAsBytes())).sync();
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        if (!completed || exc != null) {
            throw new TimeoutException();
        }
        setWasSend(true);
        setStopTime(System.nanoTime());
        setDuration(calculateDuration());
        setMessagesSent(1);
    }
    protected void closeConnection() {
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
    }


    @Override
    protected void sendData() throws TimeoutException, SSLException, InterruptedException, InterfaceDoesNotHaveIPAddressException {
        setStartTime(System.nanoTime());
        String target = useResolverDomainName ? resolverDomainName : resolver;
        initConnection(target);
        sendSingleRequest();
        closeConnection();
        Platform.runLater(() -> controller.getSendButton().setText(controller.getButtonText()));
    }

    @Override
    protected void updateProgressUI() {

    }

    @Override
    protected void updateResultUI() {
        Platform.runLater(new RequestResultsUpdateRunnable(this));
    }

    @Override
    public void stopExecution() {
        ChannelFuture future = channel.close();
        try {
            future.sync();
        } catch (InterruptedException e) {
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        }
    }
}
