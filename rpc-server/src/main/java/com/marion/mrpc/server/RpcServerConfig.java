package com.marion.mrpc.server;

import com.marion.mrpc.codec.Decoder;
import com.marion.mrpc.codec.Encoder;
import com.marion.mrpc.codec.JSONDecoder;
import com.marion.mrpc.codec.JSONEncoder;
import com.marion.mrpc.transport.HttpTransportClient;
import com.marion.mrpc.transport.HttpTransportServer;
import com.marion.mrpc.transport.TransportServer;
import lombok.Data;

/**
 * RPC Server的配置
 *      1. transportServer: 负责连接的网络通信模块
 *      2. encoder & decoder: 编码解码的序列化模块
 *      3. port端口：RPC Server启动之后监听什么端口
 */
@Data
public class RpcServerConfig {

    private Class<? extends TransportServer> transportServer = HttpTransportServer.class;

    private Class<? extends Encoder> encoder = JSONEncoder.class;

    private Class<? extends Decoder> decoder = JSONDecoder.class;

    private int port = 3000;


}
