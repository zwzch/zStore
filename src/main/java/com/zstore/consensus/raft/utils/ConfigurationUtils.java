package com.zstore.consensus.raft.utils;

import com.zstore.consensus.raft.proto.StoreProto;

public class ConfigurationUtils {
    public static boolean containsServer(StoreProto.Configuration configuration, int serverId) {
        for (StoreProto.Server server : configuration.getServersList()) {
            if (server.getServerId() == serverId) {
                return true;
            }
        }
        return false;
    }
}
