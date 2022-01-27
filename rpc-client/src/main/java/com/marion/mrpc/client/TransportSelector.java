package com.marion.mrpc.client;

import com.marion.mrpc.Peer;
import com.marion.mrpc.transport.TransportClient;

import java.util.List;

/**
 * 路由策略: 随机策略
 *      1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
 *              每个连接需要启动一个本地的transportClient作为网络通信客户端
 *      2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
 *      3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
 *      4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
 */
public interface TransportSelector {

    /**
     * 1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
     *          每个连接需要启动一个本地的transportClient作为网络通信客户端
     *
     * @param rpcServersList  可以连接的rpcServers端点列表信息
     * @param count                每个RpcClient与所有RpcServer之间, 默认建立多少连接
     * @param transportClientClazz transportClient作为网络通信客户端
     */
    void init(List<Peer> rpcServersList,
            int count,
            Class<? extends TransportClient> transportClientClazz);

    /**
     * 2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
     *
     * @return TransportClient
     */
    TransportClient select();

    /**
     * 3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
     *
     * @param transportClient 网络通信客户端
     */
    void release(TransportClient transportClient);

    /**
     * 4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
     */
    void close();

}
