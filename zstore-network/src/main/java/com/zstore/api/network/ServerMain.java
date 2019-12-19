package com.zstore.api.network;

public class ServerMain {
    public static void main(String[] args) {
        ServerFactory serverFactory = new NettyServerFactory();
        Server server = serverFactory.getServer();
        server.start();
    }
}
