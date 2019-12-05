package com.zstore.test;
import com.zstore.test.impl.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class TestNode {
    public static void main(String[] args) throws InterruptedException {

       InetSocketAddress address = new InetSocketAddress("localhost",8080);
       InetSocketAddress address1 = new InetSocketAddress("localhost",8081);
       InetSocketAddress address2 = new InetSocketAddress("localhost",8082);
       List<InetSocketAddress> addressList = new ArrayList<>();
       addressList.add(address);
       addressList.add(address1);
       addressList.add(address2);
       addressList.forEach(inetSocketAddress -> {
           new Thread(() -> {
               System.out.println(inetSocketAddress);
               Node node = new NodeImpl(inetSocketAddress);
               try {
                   node.start();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }).start();
       });

    }
}
