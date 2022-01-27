package com.marion.mrpc.client;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Peer;
import com.marion.mrpc.transport.TransportClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 路由策略: 随机策略
 *      1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
 *              每个连接需要启动一个本地的transportClient作为网络通信客户端
 *      2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
 *      3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
 *      4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
 */
@Slf4j
public class RandomTransportSelector implements TransportSelector {

    /**
     * transportClientsList中存放[已与对端rpcServer建立连接的transportClient]
     */
    private List<TransportClient> transportClientsList;

    public RandomTransportSelector() {
        // 无参构造方法 初始化时[已连接列表]为空
        this.transportClientsList = new ArrayList<>();
    }

    /**
     * 1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
     *          每个连接需要启动一个本地的transportClient作为网络通信客户端
     *
     * @param rpcServersList  可以连接的rpcServers端点列表信息
     * @param count                每个RpcClient与所有RpcServer之间, 默认建立多少连接
     * @param transportClientClazz transportClient作为网络通信客户端
     */
    @Override
    public synchronized void init(List<Peer> rpcServersList, int count, Class<? extends TransportClient> transportClientClazz) {
        count = Math.max(count, 1);

        for (Peer rpcServer : rpcServersList) {
            // 根据rpcServersList列表创建网络连接, 返回已建立连接的transportClient,
            // 并放入transportClients【List<TransportClient>】
            // 代表已建立连接的transportClient
            for (int i = 0; i < count; i++) {
                TransportClient transportClient = ReflectUtils.newInstance(transportClientClazz);
                // *. connect: client->创建连接到对端peer, 即连接rpcServer
                transportClient.connect(rpcServer);
                transportClientsList.add(transportClient);
                log.info("transportClient {}", transportClient);
            }
        }
    }

    /**
     * 2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
     *
     * @return TransportClient
     */
    @Override
    public synchronized TransportClient select() {
        int i = new Random().nextInt(transportClientsList.size());
        return transportClientsList.remove(i);
    }

    /**
     * 3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
     *
     * @param transportClient 网络通信客户端
     */
    @Override
    public synchronized void release(TransportClient transportClient) {
        transportClientsList.add(transportClient);
    }

    /**
     * 4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
     */
    @Override
    public synchronized void close() {
        for (TransportClient transportClient : transportClientsList) {
            transportClient.close();
        }
        transportClientsList.clear();
    }
}
