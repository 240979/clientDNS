package models;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import tasks.DNSTaskBase;

public class DoQClientInitializer extends ChannelInitializer<QuicStreamChannel> {
    private DNSTaskBase dnsTask;
    public DoQClientInitializer(DNSTaskBase task)
    {
        this.dnsTask = task;
    }
    @Override
    protected void initChannel(QuicStreamChannel quicStreamChannel){
        ChannelPipeline pipeline = quicStreamChannel.pipeline();
        pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        //pipeline.addLast(new DoQClientHandler(dnsTask));
        pipeline.addLast(new DnsOverSecureProtocolHandler(dnsTask));
        pipeline.addLast(new ReadTimeoutHandler(3));
    }
}
