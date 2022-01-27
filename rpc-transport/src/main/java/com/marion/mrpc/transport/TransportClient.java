package com.marion.mrpc.transport;

import com.marion.mrpc.Peer;

import java.io.InputStream;

/**
 * 网络通信客户端接口
 *      1. connect: client->创建连接到对端peer, 即连接server
 *      2. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
 *      3. close: 关闭client
 */
public interface TransportClient {

    void connect(Peer peer);

    InputStream write(InputStream data);

    void close();
}
