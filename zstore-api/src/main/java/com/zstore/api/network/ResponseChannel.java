package com.zstore.api.network;

import io.netty.buffer.ByteBuf;
import javafx.util.Callback;

public interface ResponseChannel {
    void write(ByteBuf byteBuf, Callback callback);

    void onResponseComplete(Exception exception);

    void close();

    void status();

    void setHeader();

    void getHeader();
}
