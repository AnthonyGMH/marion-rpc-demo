package com.marion.mrpc.client;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Peer;
import com.marion.mrpc.transport.TransportClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机选择一个连接
 */
@Slf4j
public class RandomTransportSelector implements TransportSelector {

    /**
     * 已连接好的TransportClient
     */
    private List<TransportClient> transportClients;

    public RandomTransportSelector() {
        // 线程安全
        this.transportClients = new ArrayList<>();
    }

    /**
     * 初始化select
     *
     * @param peerList             可以连接的server端点信息
     * @param count                client与server建立多少连接
     * @param transportClientClazz 网络通信客户端client
     */
    @Override
    public synchronized void init(List<Peer> peerList, int count, Class<? extends TransportClient> transportClientClazz) {
        count = Math.max(count, 1);

        for (Peer peer : peerList) {
            // 根据peerList列表创建网络通信客户端, 并放入transportClients【List<TransportClient>】
            // 代表已连接好的TransportClient
            for (int i = 0; i < count; i++) {
                TransportClient transportClient = ReflectUtils.newInstance(transportClientClazz);
                transportClient.connect(peer);
                transportClients.add(transportClient);
                log.info("transportClient {}", transportClient);
            }
        }
    }

    /**
     * 选择一个transport与server连接交互
     * 即是从空闲列表中剔除
     * @return TransportClient
     */
    @Override
    public synchronized TransportClient select() {
        int i = new Random().nextInt(transportClients.size());
        return transportClients.remove(i);
    }

    /**
     * 释放, 即是加入回空闲列表中
     * 返回值为空
     * @param transportClient 网络通信客户端
     */
    @Override
    public synchronized void release(TransportClient transportClient) {
        transportClients.add(transportClient);
    }

    /**
     * 关闭, 即清理transportClients列表
     */
    @Override
    public synchronized void close() {
        for (TransportClient transportClient : transportClients) {
            transportClient.close();
        }
        transportClients.clear();
    }
}
