package models;

import exceptions.HttpCodeException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamFrame;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import tasks.DNSOverHTTPS3Task;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static tasks.DNSTaskBase.LOGGER;

public class DnsOverHTTPS3Handler extends SimpleChannelInboundHandler<Http3RequestStreamFrame> {

    private final DNSOverHTTPS3Task task;

    public DnsOverHTTPS3Handler(DNSOverHTTPS3Task task) {
        this.task = task;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3RequestStreamFrame frame) {
        LOGGER.info("DoH3 frame received: " + frame.getClass().getSimpleName());
        if (frame instanceof Http3HeadersFrame headersFrame) {
            int status = Integer.parseInt(Objects.requireNonNull(headersFrame.headers().status()).toString());
            LOGGER.info("DoH3 status: " + status);
            if (status != 200) {
                task.setExc(new HttpCodeException(status));
                task.setResponseCode(status);
                task.getLatch().countDown();
                ctx.close();

            }
        } else if (frame instanceof Http3DataFrame dataFrame) {
            int readable = dataFrame.content().readableBytes();
            LOGGER.info("DoH3 DATA bytes: " + readable);
            if (readable > 0) {
                byte[] data = new byte[readable];
                dataFrame.content().readBytes(data);
                // Detect JSON by checking if first byte is '{' (0x7B)
                if (data[0] == 0x7B) {
                    LOGGER.info("Detected JSON response");
                    try {
                        String json = new String(data, StandardCharsets.UTF_8);
                        task.setHttpResponse((JSONObject) new JSONParser().parse(json));
                        task.setByteSizeResponseDoHDecompressed(data.length);
                    } catch (ParseException e) {
                        task.setExc(e);
                    }
                } else {
                    LOGGER.info("Detected wire format response");
                    task.setReceiveReply(data);
                    task.setByteSizeResponseDoHDecompressed(data.length);
                }
            }
            task.getLatch().countDown();
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.severe(ExceptionUtils.getStackTrace(cause));
        task.setExc(new Exception(cause));
        task.getLatch().countDown();
        ctx.close();
    }
}