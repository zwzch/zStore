package com.zstore.api.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_LENGTH;

public class NettyMessageProcessor extends SimpleChannelInboundHandler<HttpObject> {
    private boolean keepAlive;
    private AsyncRequestProcesser requestProcesser;
    private NettyResponseChannel responseChannel;
    private NettyRequest nettyRequest;
    private ChannelHandlerContext ctx = null;
    public NettyMessageProcessor(AsyncRequestProcesser requestProcesser) {
        this.requestProcesser = requestProcesser;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        boolean recognized = false;
        boolean success = true;
        if (msg instanceof HttpRequest) {
            resetChannel();
            HttpRequest request = (HttpRequest) msg;
            if (HttpHeaderUtil.is100ContinueExpected(request)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
            }
            keepAlive = HttpHeaderUtil.isKeepAlive(request);
            nettyRequest = new NettyRequest(request);
            requestProcesser.submitRequest(nettyRequest, responseChannel);
        }
        if(msg instanceof LastHttpContent){
//            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer("hello world".getBytes()));
//            response.headers().set(CONTENT_TYPE, "text/plain");
//            response.headers().set(CONTENT_LENGTH, response.content().readableBytes() + "");
//            if (!keepAlive) {
//                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//            } else {
//                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//                ctx.writeAndFlush(response);
//            }
        }
    }

    private void resetChannel() {
        responseChannel = new NettyResponseChannel(ctx, nettyRequest);
    }
}
