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
 * 调用远程服务的前提, 自定义动态代理类的处理.
 * 主要是重写 invoke 方法, 自定义其中的逻辑处理.
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

    // 初始化构造方法, 加载远程服务的所有信息
    public <T> RemoteInvoker(Class<T> clazz,Encoder encoder, Decoder decoder, TransportSelector selector) {
        this.clazz = clazz;
        this.encoder = encoder;
        this.decoder = decoder;
        this.selector = selector;
    }

    /**
     * 需要重写自定义的方法
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
         *          a. 初始化空的响应response & 初始化空的网络通信客户端client
         *          b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
         *          c. 将request请求序列化成byte[]二进制数组
         *          d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
         *               *. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
         *          +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
         *          e. 从IO通道中读所有可用的二进制数据, 即获取返回的响应
         *          f. 反序列化得到response类的对象
         *               catch: 日志输出异常 并处理
         *               finally: 最后将网络通信客户端transportClient释放
         *          g. 返回对应的响应response
         * 3. 【调用远程服务进行处理后】判断响应, 从响应当中拿到返回的数据
         */

        // 1. 构建Request对象
        Request request = new Request();
        request.setServiceDescriptor(ServiceDescriptor.from(clazz, method));
        request.setParameters(args);

        // 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
        Response response = invokeRemote(request);
        // 3. 【调用远程服务进行处理后】判断响应, 从响应当中拿到返回的数据
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
         *  a. 初始化空的响应response & 初始化空的网络通信客户端client
         *  b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
         *  c. 将request请求序列化成byte[]二进制数组
         *  d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
         *       *. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
         *  +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
         *  e. 从IO通道中读所有可用的二进制数据, 即获取返回的响应
         *  f. 反序列化得到response类的对象
         *      catch: 日志输出异常 并处理
         *      finally: 最后将网络通信客户端transportClient释放
         *  g. 返回对应的响应response
         */

        // a. 初始化空的响应response & 初始化空的网络通信客户端client
        Response response = null;
        TransportClient transportClient = null;

        try {
            // b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
            transportClient = selector.select();
            // c. 将request请求序列化成byte[]二进制数组
            byte[] bytesRequest = encoder.encode(request);
            // d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
            //      *. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
            InputStream afterSendRequest = transportClient.write(new ByteArrayInputStream(bytesRequest));
            // +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
            // e. 从IO通道中读所有可用的二进制数据, 即获取返回的响应
            byte[] bytesResponse = IOUtils.readFully(afterSendRequest, afterSendRequest.available());
            // f. 反序列化得到response类的对象
            response = decoder.decode(bytesResponse, Response.class);
        } catch (Exception e) {
            // catch: 日志输出异常 并处理
            log.warn("[invokeRemote] e={}, {}", e.getMessage(), e);
            response = new Response();
            response.setCode(1);
            response.setMessage("RpcClient error" + e.getClass() + ":" +e.getMessage());
        } finally {
            // finally: 最后将网络通信客户端transportClient释放
            if (transportClient != null) {
                selector.release(transportClient);
            }
        }
        // g. 返回对应的响应response
        return response;
    }




}
