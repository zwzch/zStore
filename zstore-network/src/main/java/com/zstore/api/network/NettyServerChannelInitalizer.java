package com.zstore.api.network;

import com.zstore.api.config.NettyConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyServerChannelInitalizer extends ChannelInitializer<SocketChannel> {
    AsyncRequestProcesser requestProcesser;
    public NettyServerChannelInitalizer(AsyncRequestProcesser requestProcesser) {
        this.requestProcesser = requestProcesser;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("codec", new HttpServerCodec(NettyConfig.NETTY_SERVER_MAX_INITIAL_LINE_LENGTH, NettyConfig.NETTY_SERVER_MAX_HEADER_SIZE,
                NettyConfig.NETTY_SERVER_MAX_CHUNK_SIZE))
                .addLast("idleStateHandler", new IdleStateHandler(0, 0, NettyConfig.NETTY_SERVER_IDLE_TIME_SECONDS))
                .addLast("chunker", new ChunkedWriteHandler())
                .addLast("processor", new NettyMessageProcessor(requestProcesser));
    }
}
