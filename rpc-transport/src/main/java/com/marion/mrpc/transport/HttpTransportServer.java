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
 * 基于HTTP的服务端
 * 1. 启动、监听端口
 * 2. 响应接受请求（进行处理并返回）
 * 3. 关闭
 */
@Slf4j public class HttpTransportServer implements TransportServer {

    private RequestHandler handler;

    private Server server;

    @Override public void init(int port, RequestHandler requestHandler) {
        this.handler = requestHandler;
        this.server = new Server(port);
        // Servlet接受请求
        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);
        // Servletholder是处理网络请求的抽象 // RequestServlet中处理了请求
        ServletHolder servletHolder = new ServletHolder(new RequestServlet());
        handler.addServlet(servletHolder, "/*");
    }

    @Override public void start() {
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            log.error("server start error, {}, {}", e.getMessage(), e);
        }
    }

    @Override public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.error("server stop error, {}, {}", e.getMessage(), e);
        }

    }

    // 处理请求
    class RequestServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
            // 首先拿到数据
            InputStream inputStream = req.getInputStream();
            // 处理完后返回
            OutputStream outputStream = resp.getOutputStream();
            if (handler != null) {
                handler.onRequest(inputStream, outputStream);
            }
            outputStream.flush();
        }
    }
}
