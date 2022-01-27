package com.marion.mrpc.transport;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * RPC服务端对于请求的处理过程
 * onRequest: RPC服务端从[收到请求receive]到[响应返回toResponse]中间的处理过程
 */
public interface RequestHandler {

    void  onRequest(InputStream receive, OutputStream toResponse);
}
