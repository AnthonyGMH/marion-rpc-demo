package com.marion.mrpc.transport;

import com.marion.mrpc.Peer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 基于HTTP的客户端
 * 1，创建连接
 * 2. 发送请求（获取响应）
 * 3. 关闭连接
 */
public class HttpTransportClient implements TransportClient {

    private String url;
    // 请求地址
    @Override public void connect(Peer peer) {
        this.url = "http://" + peer.getHost() + ":" + peer.getPort();
    }

    @Override public InputStream write(InputStream data) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection)new URL(url).openConnection();
            // 对连接设置属性: 需要读, 需要写. 不需要cache, 方法为POST
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            // 进行连接, 连接完后发送data出去给Server
            urlConnection.connect();
            IOUtils.copy(data, urlConnection.getOutputStream());
            // 获取HTTP返回响应码进行判断, 成功的话拿输出, 失败的话就拿错误信息
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
