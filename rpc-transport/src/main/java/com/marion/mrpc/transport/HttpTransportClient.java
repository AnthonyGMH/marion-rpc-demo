package com.marion.mrpc.transport;

import com.marion.mrpc.Peer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 基于HTTP连接的网络通信客户端
 *      1. connect: client->创建连接到对端peer, 即连接server
 *      2. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
 *      3. close: 关闭client
 */
public class HttpTransportClient implements TransportClient {

    private String url;

    // 1. client: 创建连接到对端peer, 即连接server
    @Override public void connect(Peer peer) {
        this.url = "http://" + peer.getHost() + ":" + peer.getPort();
    }

    // 2. client: 发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到InputStream二进制响应
    @Override public InputStream write(InputStream data) {
        try {
            // client: 建立与server之间的HTTP连接, 并打开.
            HttpURLConnection urlConnection = (HttpURLConnection)new URL(url).openConnection();
            // 对该HTTP连接设置属性: 需要读, 需要写. 不需要cache, 方法为POST
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            // client: 进行连接, 并发送二进制数据data出去给server
            urlConnection.connect();
            IOUtils.copy(data, urlConnection.getOutputStream());
            // 获取该HTTP连接返回的响应码进行判断, 成功的话getInputStream, 失败的话就getErrorStream
            int resultCode = urlConnection.getResponseCode();
            if (resultCode == HttpURLConnection.HTTP_OK) {
                return urlConnection.getInputStream();
            } else {
                return urlConnection.getErrorStream();
            }
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    @Override public void close() {

    }
}
