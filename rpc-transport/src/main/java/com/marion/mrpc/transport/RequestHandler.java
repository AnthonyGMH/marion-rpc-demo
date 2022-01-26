package com.marion.mrpc.transport;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 处理请求的handler
 * 输入数据的通道 & 输出数据的通道
 */
public interface RequestHandler {

    void  onRequest(InputStream receive, OutputStream toResponse);
}
