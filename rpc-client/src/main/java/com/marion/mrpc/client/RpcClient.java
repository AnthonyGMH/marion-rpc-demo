package com.marion.mrpc.client;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.codec.Decoder;
import com.marion.mrpc.codec.Encoder;

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
            this.config.getRpcServers(),
            this.config.getConnectCount(),
            this.config.getTransportClass()
        );
    }


    /**
     * 获取接口的代理对象 需要new定义一个RemoteInvoke对象传入
     * RemoteInvoker: 调用远程服务的前提, 自定义动态代理类的处理.
     * @param interfaceClass 需要代理的接口类
     * @param <T> 泛型
     * @return 返回代理对象T
     */
    public <T> T getProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{interfaceClass},
            new RemoteInvoker(interfaceClass, encoder, decoder, selector)
        );
    }
}
