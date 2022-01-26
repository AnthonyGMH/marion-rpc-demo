package com.marion.mrpc.transport;

import com.marion.mrpc.Peer;

import java.io.InputStream;

/**
 * 传输客户端接口
 *      1. 创建连接
 *      2. 发送请求（获取响应）
 *      3. 关闭连接
 */
public interface TransportClient {

    void connect(Peer peer);

    InputStream write(InputStream stream);

    void close();
}
