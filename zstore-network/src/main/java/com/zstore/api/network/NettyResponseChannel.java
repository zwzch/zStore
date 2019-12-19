package com.zstore.api.network;

import com.zstore.api.utils.CommonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.handler.codec.http.*;
import javafx.util.Callback;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_LENGTH;

public class NettyResponseChannel implements ResponseChannel{
    static final String ERR_REASON_HEADER = "x-zstore-failure-reason";
    static final String ERR_CODE_HEADER = "x-zstore-error-code";
    private ChannelHandlerContext ctx;
    private NettyRequest request;
    private ChannelProgressivePromise writeFuture;
    public NettyResponseChannel(ChannelHandlerContext ctx, NettyRequest request) {
        this.ctx = ctx;
        this.request = request;
        writeFuture = ctx.newProgressivePromise();
    }
    @Override
    public void write(ByteBuf byteBuf, Callback callback) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes() + "");
        ctx.writeAndFlush(response);
        onResponseComplete(null);
    }

    @Override
    public void onResponseComplete(Exception exception) {
        if (exception != null) {
            if (!sendErrorResponse(exception)) {
                completeRequest(true);
            }
        }
        if (!writeFuture.isDone()) {
            writeFuture.setFailure(exception);
        }
    }

    private void completeRequest(boolean colseChannel) {
        if (colseChannel && ctx.channel().isOpen()) {
            ctx.channel().close();
        }
    }

    private boolean sendErrorResponse(Exception exception) {
        FullHttpResponse errResp = generateErrorResp(exception);
        this.ctx.writeAndFlush(errResp);
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public void status() {

    }

    @Override
    public void setHeader() {

    }

    @Override
    public void getHeader() {

    }

    private FullHttpResponse generateErrorResp(Throwable cause){
        HttpVersion version;
        HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        CommonUtils.getRootCause(cause);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(ERR_CODE_HEADER, "0000");
        response.headers().set(ERR_REASON_HEADER, response.toString());
        return response;
    }
}
