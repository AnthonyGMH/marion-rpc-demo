package com.marion.mrpc.client;

import com.marion.mrpc.Peer;
import com.marion.mrpc.transport.TransportClient;

import java.util.List;

/**
 * 表示选择哪个server去连接
 */
public interface TransportSelector {

    /**
     * 初始化select
     * @param peerList 可以连接的server端点信息
     * @param count client与server建立多少连接
     * @param transportClientClazz 网络通信客户端client
     */
    void init(List<Peer> peerList,
            int count,
            Class<? extends TransportClient> transportClientClazz);

    /**
     * 选择一个transport与server连接交互
     * @return TransportClient
     */
    TransportClient select();

    /**
     * 释放
     * @param transportClient 网络通信客户端
     */
    void release(TransportClient transportClient);

    void close();

}
