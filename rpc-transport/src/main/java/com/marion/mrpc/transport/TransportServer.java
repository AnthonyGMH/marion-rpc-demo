package com.marion.mrpc.transport;

/**
 *  网络通信服务端接口
 *      1. init: 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
 *      2. start: 启动jettyServer, 并等待接收请求, 最终[RequestServlet负责]响应进行处理并返回
 *      3. stop: 关闭jettyServer
 */
public interface TransportServer {


    void init(int port, RequestHandler requestHandler);

    void start();

    void stop();
}
