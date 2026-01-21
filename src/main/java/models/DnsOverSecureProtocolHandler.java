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
        /*byte[] responseBytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(responseBytes);
        System.out.println("Received response: " + Arrays.toString(responseBytes));
        */
        ByteBuf dataLengthBuf = byteBuf.readBytes(2);
        byte[] dataLengthByte = new byte[dataLengthBuf.readableBytes()];
        dataLengthBuf.readBytes(dataLengthByte);
        int length = ((dataLengthByte[0] & 0xFF) << 8) + (dataLengthByte[1] & 0xFF);
        ByteBuf dnsBuf = byteBuf.readBytes(length);
        byte[] pck = new byte[dnsBuf.readableBytes()];
        dnsBuf.readBytes(pck);
        task.setReceiveReply(pck);
        switch (task) {
            case DNSOverTLS dnsOverTLS -> dnsOverTLS.setNotFinished(false);
            case DNSOverQUICTask dnsOverQUICTask -> dnsOverQUICTask.setNotFinished(false);
            case null, default -> {
            }
        }
    }
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //cause.printStackTrace();
        // LOGGER.severe(cause.toString());
        LOGGER.severe(ExceptionUtils.getStackTrace(cause));
        ctx.close();
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOGGER.info("chanelRead");
        //System.out.println("channelRead() - received: " + msg.getClass().getName() + ", " + msg);
        this.channelRead0(ctx, (ByteBuf) msg);
        //super.channelRead(ctx, msg);
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Stream channel active");
        //System.out.println("Stream channel active");
        super.channelActive(ctx);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Stream channel inactive");
        //System.out.println("Stream channel inactive");
        super.channelInactive(ctx);
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelReadComplete() called");
        //System.out.println("channelReadComplete() called");
        super.channelReadComplete(ctx);
    }
}
