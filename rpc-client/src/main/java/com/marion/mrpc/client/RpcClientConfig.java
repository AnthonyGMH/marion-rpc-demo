package com.marion.mrpc.client;

import com.marion.mrpc.Peer;
import com.marion.mrpc.codec.Decoder;
import com.marion.mrpc.codec.Encoder;
import com.marion.mrpc.codec.JSONDecoder;
import com.marion.mrpc.codec.JSONEncoder;
import com.marion.mrpc.transport.HttpTransportClient;
import com.marion.mrpc.transport.TransportClient;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * RPC Client的配置
 *    1. TransportClient: 选择网络通信模块，具体采用什么样的连接
 *    2. Encoder & Decoder: 选择编码解码序列化模块，序列化采用什么格式
 *    3. TransportSelector：选择路由的策略，默认随机策略
 *    4. connectCount连接数: 每个RpcClient与所有RpcServer之间, 默认建立多少连接
 *    5. RpcServers: 初始化默认有的服务器 ip & 端口
 */
@Data
public class RpcClientConfig {


    private Class<? extends TransportClient> transportClass = HttpTransportClient.class;

    private Class<? extends Encoder> encoder = JSONEncoder.class;

    private Class<? extends Decoder> decoder = JSONDecoder.class;

    private Class<? extends TransportSelector> transportSelector = RandomTransportSelector.class;

    // 默认建立一个连接
    private int connectCount = 1;
    // 默认本地3000端口
    private List<Peer> rpcServers = Arrays.asList(new Peer("127.0.0.1", 3000));
}
