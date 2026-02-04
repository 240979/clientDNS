/*
    Deprecated by DnsOverSecureProtocolHandler
*/
package models;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSTaskBase;

import static tasks.DNSTaskBase.LOGGER;

public class DoQClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private DNSTaskBase dnsTaskBase;

    public DoQClientHandler(DNSTaskBase dnsTask){
        this.dnsTaskBase = dnsTask;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        LOGGER.info("chanelRead0");
        ByteBuf dataLengthBuf = byteBuf.readBytes(2);
        byte[] dataLengthByte = new byte[dataLengthBuf.readableBytes()];
        dataLengthBuf.readBytes(dataLengthByte);
        int length = ((dataLengthByte[0] & 0xFF) << 8) + (dataLengthByte[1] & 0xFF);
        ByteBuf dnsBuf = byteBuf.readBytes(length);
        byte[] pck = new byte[dnsBuf.readableBytes()];
        dnsBuf.readBytes(pck);
        dnsTaskBase.setReceiveReply(pck);
    }
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.severe(ExceptionUtils.getStackTrace(cause));
        ctx.close();
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.info("channelRead() - received: " + msg.getClass().getName() + ", " + msg);
        super.channelRead(ctx, msg);
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Stream channel active");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Stream channel inactive");
        super.channelInactive(ctx);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelReadComplete() called");
        super.channelReadComplete(ctx);
    }
}
