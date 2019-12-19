package com.zstore.api.network;

import com.zstore.api.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRequestProcesser{
    private final static int workerNum = 2;
    private List workers = new ArrayList(workerNum);
    private final AtomicInteger currIndex = new AtomicInteger(0);

    public void startup(){
        for (int i = 0; i < workerNum; i++) {
            RequestHandler requestHandler = new RequestHandler();
            workers.add(requestHandler);
            ThreadUtils.newThread(requestHandler, "RequestWorker", false).start();
        }
    }

    public void shutdown(){

    }

    public void submitRequest(NettyRequest request, NettyResponseChannel responseChannel) {
        AsyncRequestInfo asyncRequestInfo = new AsyncRequestInfo(request, responseChannel);
        getWorker().submitRequest(asyncRequestInfo);
    }
    private RequestHandler getWorker() {
        int autoIndex = currIndex.getAndIncrement();
        int realIndex = autoIndex % workerNum;
        return (RequestHandler) workers.get(realIndex);
    }
}
