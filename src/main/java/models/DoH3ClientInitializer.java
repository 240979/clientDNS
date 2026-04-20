/*
 * Created by 240979
 * Based on: https://github.com/xramos00/DNS_client
 *           https://github.com/mbio16/clientDNS
 */
package models;

import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import tasks.DNSOverHTTPS3Task;

public class DoH3ClientInitializer extends Http3RequestStreamInitializer {

    private final DNSOverHTTPS3Task task;

    public DoH3ClientInitializer(DNSOverHTTPS3Task task) {
        this.task = task;
    }

    @Override
    protected void initRequestStream(QuicStreamChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new DnsOverHTTPS3Handler(task));
    }
}