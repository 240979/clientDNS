/*
    This is a class is basically DoTClientHandler, but changed to be used by DoQ also.
    So, I, Roman Szymutko, am not author of the conversion logic.
*/

package models;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSOverQUICTask;
import tasks.DNSOverTLS;
import tasks.DNSTaskBase;

import static tasks.DNSTaskBase.LOGGER;

public class DnsOverSecureProtocolHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final DNSTaskBase task;

    public DnsOverSecureProtocolHandler(DNSTaskBase dnsTask){
        this.task = dnsTask;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        LOGGER.info("chanelRead0");
        ByteBuf dataLengthBuf = byteBuf.readBytes(2);
        byte[] dataLengthByte = new byte[dataLengthBuf.readableBytes()];
        dataLengthBuf.readBytes(dataLengthByte);
        int length = ((dataLengthByte[0] & 0xFF) << 8) + (dataLengthByte[1] & 0xFF);
        ByteBuf dnsBuf = byteBuf.readBytes(length);
        byte[] pck = new byte[dnsBuf.readableBytes()];
        dnsBuf.readBytes(pck);
        task.setReceiveReply(pck);

        switch (task) {
            case DNSOverTLS dnsOverTLS -> {
                dnsOverTLS.getLatch().countDown();
            }
            case DNSOverQUICTask dnsOverQUICTask ->{
                dnsOverQUICTask.getLatch().countDown();
            }
            case null, default -> {
            }
        }
        channelHandlerContext.close();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.severe(ExceptionUtils.getStackTrace(cause));
        switch (task) {
            case DNSOverTLS dnsOverTLS -> {
                dnsOverTLS.setExc(new Exception(cause));
                dnsOverTLS.getLatch().countDown();
            }
            case DNSOverQUICTask dnsOverQUICTask -> {
                dnsOverQUICTask.setExc(new Exception(cause));
                dnsOverQUICTask.getLatch().countDown();
            }
            case null, default -> {}
        }
        ctx.close();
    }
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        LOGGER.info("chanelRead");
//        this.channelRead0(ctx, (ByteBuf) msg);
//    }
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
