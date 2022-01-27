package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC框架中请求与响应之间规定的协议。
 * 客户端请求ServiceDescriptor，parameters
 * 作用：作为在RPC中[客户端发出&服务端收到]的处理请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request {

    /**
     * @param serviceDescriptor 请求的服务【描述服务：服务即一个对外的方法】
     * @param parameters 请求的参数数组
     */
    private ServiceDescriptor serviceDescriptor;
    private Object[] parameters;
}
