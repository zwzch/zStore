package com.zstore.test.impl;

import com.zstore.test.Node;
import com.zstore.test.Server;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerImpl implements Server {

    private ServerSocket socket;

    @Override
    public void run(Node node) throws IOException, InterruptedException {
        node.start();
    }

    @Override
    public void stop() {

    }
}
