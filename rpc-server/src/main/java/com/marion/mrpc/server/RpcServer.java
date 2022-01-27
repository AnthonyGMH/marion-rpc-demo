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


    // 无参构造方法
    public RpcServer() {
        this(new RpcServerConfig());
    }

    // 有参构造方法, 加载配置类
    public RpcServer(RpcServerConfig config) {
        // 配置
        this.config = config;
        // 网络通信模块 通过反射工具类ReflectUtils 并初始化
        this.transportServer = ReflectUtils.newInstance(config.getTransportServer());
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

        /**
         * 1. 从IO通道中读所有可用的二进制数据, 即获取收到的请求
         * 2. 反序列化得到约定协议格式的请求request
         * 3. 对ServiceManager传入request, 找到对外提供的具体服务实例ServiceInstance
         * 4. ServiceInstance的invoke方法, 通过反射工具类ReflectUtils调用对应的具体方法, 得到结果invokeResult【Object类-所有可能的数据】
         * 5. 将结果invokeResult写入约定格式的响应response中去, 【还不是二进制数据】最终需要序列化发送回去
         *      catch: 日志输出异常 并处理
         *      finally:
         *      6. 将【响应请求并处理返回的】response序列化成二进制数据
         *      7. 往响应返回流中写入二进制数据
         * @param receiveRequest 收到的请求
         * @param toResponse 返回的响应
         */




        @Override
        public void onRequest(InputStream receiveRequest, OutputStream toResponse) {
            try {
                // 1. 从IO通道中读所有可用的二进制数据, 即获取收到的请求
                byte[] bytesReceiveRequest = IOUtils.readFully(receiveRequest, receiveRequest.available());
                // 2. 反序列化得到约定协议格式的请求request
                Request request = decoder.decode(bytesReceiveRequest, Request.class);
                log.info("get request, {}", request);
                // 3. 对ServiceManager传入request, 找到对外提供的具体服务实例ServiceInstance
                ServiceInstance serviceInstance = serviceManager.lookup(request);
                log.info("get service, {}", serviceInstance);
                // 4. ServiceInstance的invoke方法, 通过反射工具类ReflectUtils调用对应的具体方法, 得到结果invokeResult【Object类-所有可能的数据】
                Object invokeResult = serviceInvoker.invoke(serviceInstance, request);
                // 5. 将结果invokeResult写入约定格式的响应response中去, 【还不是二进制数据】最终需要序列化发送回去
                response.setData(invokeResult);

            } catch (Exception e) {
                // catch: 日志输出异常 并处理
                log.warn(e.getMessage(), e);
                // 响应中发返回 1-失败码 并返回对应的错误信息
                response.setCode(1);
                response.setMessage("RpcServer get error: " + e.getClass().getName());
            } finally {
                // finally:
                // 二进制数组
                byte[] byteResponse = new byte[0];
                try {
                    // 6. 将【响应请求并处理返回的】response序列化成二进制数据
                    byteResponse = encoder.encode(response);
                    // 7. 往响应返回流中写入二进制数据
                    toResponse.write(byteResponse);
                    log.info("RpcServer response");
                } catch (Exception e) {
                    log.warn("onRequest {}, {}", e.getMessage(), e);
                }
            }
        }
    };


}
