package com.marion.mrpc.transport;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *  基于HTTP连接的网络通信服务端
 *      1. init: 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
 *      2. start: 启动jettyServer, 并等待接收请求, 最终[RequestServlet负责]响应进行处理并返回
 *      3. stop: 关闭jettyServer
 */
@Slf4j public class HttpTransportServer implements TransportServer {

    private RequestHandler requestHandler;

    private Server jettyServer;

    //  1. 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
    @Override public void init(int port, RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.jettyServer = new Server(port);

        // 对jettyServer的一些补充处理, 主要是设置Servlet
        ServletContextHandler handler = new ServletContextHandler();
        // ServletHolder是处理网络请求的抽象 // RequestServlet中处理了请求
        ServletHolder servletHolder = new ServletHolder(new RequestServlet());
        handler.addServlet(servletHolder, "/*");
        // 将上述放到jettyServer当中
        jettyServer.setHandler(handler);
    }

    @Override public void start() {
        try {
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
            log.error("server start error, {}, {}", e.getMessage(), e);
        }
    }

    @Override public void stop() {
        try {
            jettyServer.stop();
        } catch (Exception e) {
            log.error("server stop error, {}, {}", e.getMessage(), e);
        }

    }

    /**
     * 对于请求的处理过程
     */
    class RequestServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            // 拿到[接收请求receive]的通道
            InputStream receive = request.getInputStream();
            // 拿到[响应返回toResponse]的通道
            OutputStream toResponse = response.getOutputStream();
            // RPC服务端从[收到请求receive]到[响应返回toResponse]中间的处理过程
            if (requestHandler != null) {
                requestHandler.onRequest(receive, toResponse);
            }
            toResponse.flush();
        }
    }
}
