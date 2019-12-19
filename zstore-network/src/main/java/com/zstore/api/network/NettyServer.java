package com.zstore.api.network;

import com.zstore.api.config.NettyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NettyServer implements Server{
    public static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private final ChannelInitializer<SocketChannel> initializer;
    private AsyncRequestProcesser requestProcesser;
    public NettyServer(ChannelInitializer<SocketChannel> initializer) {
        this.initializer = initializer;
    }

    @Override
    public void start() {
        long startTime = System.currentTimeMillis();
        bossGroup = new NioEventLoopGroup(NettyConfig.NETTY_SERVER_BOSS_THREAD_COUNT);
        workGroup = new NioEventLoopGroup(NettyConfig.NETTY_SERVER_WORKER_THREAD_COUNT);
        try {
            Channel channel = bindServer(workGroup, bossGroup, initializer, NettyConfig.NETTY_SERVER_PORT);
            channel.closeFuture().sync();
            log.info("NettyServer now listening on port {}", NettyConfig.NETTY_SERVER_PORT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Channel bindServer(EventLoopGroup bossGroup, EventLoopGroup workGroup, ChannelInitializer<SocketChannel> initializer, int port) throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, NettyConfig.NETTY_SERVER_SO_BACKLOG)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(initializer);
        return b.bind(port).sync().channel();
    }
    @Override
    public void stop() {

    }
}
