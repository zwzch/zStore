package com.zstore.test.impl;

import com.zstore.test.Node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

public class NodeImpl implements Node {
    public static final String RUNNING = "running";
    public static final String UNAVAILABLE = "unavailable";
    private String state;
    private InetSocketAddress address;
    private ServerSocket server;
    private List<InetSocketAddress> addressList;
    public NodeImpl(InetSocketAddress address) {
        this.state = RUNNING;
        this.address = address;
        this.addressList = addressList;
        try {
            server = new ServerSocket(address.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void sync() {

        System.out.println(this.state);
    }

    @Override
    public void start() throws InterruptedException {
        while (true) {
            System.out.println(this.address.getPort() + "==>" + state);
            Thread.sleep(3000);
        }
    }
}
