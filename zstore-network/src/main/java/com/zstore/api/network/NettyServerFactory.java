package com.zstore.api.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;


public class NettyServerFactory implements ServerFactory {
    ChannelInitializer<SocketChannel> channelInitializer;
    AsyncRequestProcesser requestProcesser;
    public NettyServerFactory() {
        this.requestProcesser = new AsyncRequestProcesser();
        this.requestProcesser.startup();
        this.channelInitializer = new NettyServerChannelInitalizer(requestProcesser);
    }

    @Override
    public Server getServer() {
        return new NettyServer(this.channelInitializer);
    }
}
