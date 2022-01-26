package com.marion.mrpc.client;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.codec.Decoder;
import com.marion.mrpc.codec.Encoder;
import com.marion.mrpc.codec.JSONDecoder;
import com.marion.mrpc.codec.JSONEncoder;

import java.lang.reflect.Proxy;

public class RpcClient {

    private RpcClientConfig config;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector selector;

    // 无参构造方法
    public RpcClient() {
        this(new RpcClientConfig());
    }

    // 有参构造方法, 加载配置类
    public RpcClient(RpcClientConfig config) {
        this.config = config;

        this.encoder = ReflectUtils.newInstance(this.config.getEncoder());
        this.decoder = ReflectUtils.newInstance(this.config.getDecoder());

        this.selector = ReflectUtils.newInstance(this.config.getTransportSelector());

        this.selector.init(
            this.config.getServers(),
            this.config.getConnectCount(),
            this.config.getTransportClass()
        );
    }

    /**
     * 获取接口的代理对象 需要new定义一个RemoteInvoke
     * @param clazz 需要代理的类
     * @param <T> 泛型
     * @return 代理对象T
     */
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{clazz},
            new RemoteInvoker(clazz, encoder, decoder, selector)
        );
    }
}
