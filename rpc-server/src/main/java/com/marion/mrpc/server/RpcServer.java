package com.marion.mrpc.server;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Request;
import com.marion.mrpc.Response;
import com.marion.mrpc.codec.Decoder;
import com.marion.mrpc.codec.Encoder;
import com.marion.mrpc.transport.RequestHandler;
import com.marion.mrpc.transport.TransportServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * RPC服务端
 */
@Slf4j
public class RpcServer {

    /**
     * 配置信息
     */
    private RpcServerConfig config;             // 配置信息
    private TransportServer transportServer;    // 网络通信模块
    private Encoder encoder;                    // 序列化模块-序列化
    private Decoder decoder;                    // 序列化模块-反序列化
    private ServiceManager serviceManager;      // 服务管理模块
    private ServiceInvoker serviceInvoker;      // 服务调用模块



        public RpcServer() {
        this(new RpcServerConfig());
    }

    public RpcServer(RpcServerConfig config) {
        // 配置
        this.config = config;
        // 网络通信模块 通过反射工具类ReflectUtils 并初始化
        this.transportServer = ReflectUtils.newInstance(config.getTransport());
        this.transportServer.init(config.getPort(), this.handler);
        // 序列化模块 通过反射工具类ReflectUtils
        this.encoder = ReflectUtils.newInstance(config.getEncoder());
        this.decoder = ReflectUtils.newInstance(config.getDecoder());
        // 初始化服务
        this.serviceManager = new ServiceManager();
        this.serviceInvoker = new ServiceInvoker();
    }

    // 启动即是网络通信模块启动, 并开启监听
    public void start() {
        this.transportServer.start();
    }

    // 关闭即是网络通信模块关闭, 并关闭监听
    public void stop() {
        this.transportServer.stop();
    }

    // 注册服务 其实就是调用ServiceManager中的register方法 需要什么参数就对应传入
    public <T> void register(Class<T> interfaceClass, T bean) {
        serviceManager.register(interfaceClass, bean);
    }

    /**
     * 处理http请求，加解码
     */
    private RequestHandler handler = new RequestHandler() {

        Response response = new Response();

        @Override
        public void onRequest(InputStream receive, OutputStream toResponse) {
            try {
                // 1. 读所有可用的二进制数据
                byte[] bytes = IOUtils.readFully(receive, receive.available());
                // 2. 反序列化得到request类的对象
                Request request = decoder.decode(bytes, Request.class);
                log.info("get request, {}", request);
                // 3. 通过request类的对象, 还原具体的服务请求, 找到服务ServiceInstance
                ServiceInstance serviceInstance = serviceManager.lookup(request);
                log.info("get service, {}", serviceInstance);
                // 4. 通过ServiceInstance可以来调用具体的方法, 得到结果invoke【Object类-代表所有可能的数据】
                Object invoke = serviceInvoker.invoke(serviceInstance, request);
                // 5. 将结果invoke写入响应response中去, 并发送回去【还不是二进制数据】
                response.setData(invoke);

            } catch (Exception e) {
                // 日志输出异常
                log.warn(e.getMessage(), e);
                // 响应中发返回 1-失败码 并返回对应的错误信息
                response.setCode(1);
                response.setMessage("RpcServer get error: " + e.getClass().getName());
            } finally {
                // 6. 【响应返回的】二进制数组
                byte[] byteResponse = new byte[0];
                try {
                    // 7. 将response序列化成二进制数据
                    byteResponse = encoder.encode(response);
                    // 8. 往响应返回流中写入二进制数据
                    toResponse.write(byteResponse);
                    log.info("RpcServer response");
                } catch (Exception e) {
                    log.warn("onRequest {}, {}", e.getMessage(), e);
                }
            }
        }
    };


}
