package com.zstore.test;

import java.io.IOException;

public interface Server {
    void run(Node node) throws IOException, InterruptedException;
    void stop();
}
