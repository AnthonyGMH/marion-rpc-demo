package com.marion.mrpc.transport;

/**
 *  传输服务端
 *      1. 启动、监听端口
 *      2. 响应接受请求（进行处理并返回）
 *      3. 关闭
 */
public interface TransportServer {


    void init(int port, RequestHandler requestHandler);

    void start();

    void stop();
}
