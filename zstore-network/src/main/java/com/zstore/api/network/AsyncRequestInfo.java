package com.zstore.api.network;

public  class AsyncRequestInfo {
    private final NettyRequest restRequest;
    private final NettyResponseChannel restResponseChannel;
    private long queueStartTime = System.currentTimeMillis();

    public AsyncRequestInfo(NettyRequest restRequest, NettyResponseChannel restResponseChannel) {
        this.restRequest = restRequest;
        this.restResponseChannel = restResponseChannel;
    }

    public NettyRequest getRestRequest() {
        return restRequest;
    }

    public NettyResponseChannel getRestResponseChannel() {
        return restResponseChannel;
    }

    public long getProcessingDelay() {
      return System.currentTimeMillis() - queueStartTime;
    }
}
