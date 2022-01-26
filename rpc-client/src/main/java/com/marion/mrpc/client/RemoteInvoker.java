package com.marion.mrpc.client;

import com.marion.mrpc.Request;
import com.marion.mrpc.Response;
import com.marion.mrpc.ServiceDescriptor;
import com.marion.mrpc.codec.*;
import com.marion.mrpc.transport.TransportClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 调用远程服务，动态代理类的处理器
 */
@Slf4j
public class RemoteInvoker implements InvocationHandler {
    /**
     * 定义远程服务的所有信息
     */
    private Class clazz;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector selector;

    // 初试话构造方法, 加载远程服务的所有信息
    public <T> RemoteInvoker(Class<T> clazz,Encoder encoder, Decoder decoder, TransportSelector selector) {
        this.clazz = clazz;
        this.encoder = encoder;
        this.decoder = decoder;
        this.selector = selector;
    }

    /**
     * 需要重写的方法
     * @param proxy 动态代理
     * @param method 需要调用的方法
     * @param args 调用方法使用的参数
     * @return Object对象
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        /**
         * 1. 构建Request对象
         * 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
         * 3. 【调用远程服务进行处理后】从响应当中拿到返回的数据
         * 3. 处理得到返回结果
         */

        // 1. 构建Request对象
        Request request = new Request();
        request.setServiceDescriptor(ServiceDescriptor.from(clazz, method));
        request.setParameters(args);

        // 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
        Response response = invokeRemote(request);
        // 3. 【调用远程服务进行处理后】从响应当中拿到返回的数据
        // 调用失败
        if (response == null || response.getCode() != 0) {
            throw new IllegalStateException("fail invoke remote " + response);
        }
        // 调用成功
        return response.getData();
    }


    // 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
    private Response invokeRemote(Request request) {

        /**
         * 1. 获得一个选择器selector
         * 2. 编码请求
         * 3. 调用远程服务
         * 4. 获得返回
         * 5. 解码结果
         * 6. 处理响应
         */

        // 1. 初始化空的响应response & 初始化空的网络通信客户端client
        Response response = null;
        TransportClient client = null;

        try {
            // 2. 通过默认的路由策略selector, 选择一个网络通信客户端client
            client = selector.select();
            // 3. 将request请求序列化成二进制数组
            byte[] byteRequest = encoder.encode(request);
            // 4. 并通过网络通信客户端client发送请求, 并获得对应的响应结果
            InputStream afterSendRequest = client.write(new ByteArrayInputStream(byteRequest));
            // +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
            // 5. 读所有可用的二进制数据
            byte[] bytes = IOUtils.readFully(afterSendRequest, afterSendRequest.available());
            // 6. 反序列化得到response类的对象
            response = decoder.decode(bytes, Response.class);
        } catch (Exception e) {
            // 日志输出异常
            log.warn("[invokeRemote] e={}, {}", e.getMessage(), e);
            response = new Response();
            response.setCode(1);
            response.setMessage("RpcClient error" + e.getClass() + ":" +e.getMessage());
        } finally {
            // 最后将网络通信客户端client关闭
            if (client != null) {
                selector.release(client);
            }
        }
        // 8. 返回对应的响应response
        return response;
    }




}
