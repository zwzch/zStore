package com.zstore.api.network;

import com.zstore.api.config.RouteConfig;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestHandler implements Runnable{
    public static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    private AtomicBoolean isRunning = new AtomicBoolean(true);
    private final LinkedBlockingQueue<AsyncRequestInfo> requests = new LinkedBlockingQueue<AsyncRequestInfo>();
    @Override
    public void run() {
        log.info("request handler started");
        while (getRunning()) {
            AsyncRequestInfo request = null;
            try {
                request = requests.take();
                if (request != null) {
                    onProcessReqquest(request);
                    log.info("request process complete ", request.getRestRequest().getUri());
                }
            } catch (Exception e) {
                log.error("Exception while processing request", e);
            }
        }
    }

    private void onProcessReqquest(AsyncRequestInfo asyncRequestInfo) {
        long storegeProcessingStartTime = System.currentTimeMillis();
        NettyRequest request = asyncRequestInfo.getRestRequest();
        NettyResponseChannel channel = asyncRequestInfo.getRestResponseChannel();
        try {
            switch (request.getMethod()) {
                case GET:
                    handleGet(request, channel);
                    break;
                case POST:
                    handlePost(request, channel);
                    break;
                default:
                    log.info("not support method");
                    NetWorkException exception = new NetWorkException(NetWorkException.Code.METHOD_NOT_SUPPORT, "not support method");
                    channel.onResponseComplete(exception);
            }
        } finally {
            log.info("processing request use ", System.currentTimeMillis() - storegeProcessingStartTime, request.getUri());
        }
    }

    protected boolean getRunning() {
        return isRunning.get();
    }

    public void submitRequest(AsyncRequestInfo request){
        requests.add(request);
    }

    private void handleGet(NettyRequest getReq, ResponseChannel channel){
        if (getReq.matchesOperation(RouteConfig.GET_READ)) {
            channel.write(Unpooled.wrappedBuffer("read".getBytes()), (xx) -> {
                return "";
            });
        } else {
            NetWorkException exception = new NetWorkException(NetWorkException.Code.PATH_NOT_FOUND, "path not found");
            channel.onResponseComplete(exception);
        }
    }

    private void handlePost(NettyRequest postReq, ResponseChannel channel) {
        if (postReq.matchesOperation(RouteConfig.POST_CHECK)) {
            channel.write(Unpooled.wrappedBuffer("check".getBytes()), (xx) -> {
                return "";
            });
        } else {
            NetWorkException exception = new NetWorkException(NetWorkException.Code.PATH_NOT_FOUND, "path not found");
            channel.onResponseComplete(exception);
        }

    }
}
