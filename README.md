# Marion-RPC-demo
从0到1手写一个基于Java的轻量级RPC框架。

## 设计思路 & 模块说明
1. rpc-common：公用类 & 公用方法
2. rpc-codec：编码解码的序列化模块
3. rpc-proto：请求与响应的交互协议模块
4. rpc-transport：负责连接的网络通信模块
5. rpc-server：服务端
6. rpc-client：客户端

## 版本信息
```xml
    <properties>
        <java.version>1.8</java.version>
        <junit.version>4.8.2</junit.version>
        <lombok.version>1.18.22</lombok.version>
        <slf4j.version>1.7.30</slf4j.version>
        <logback.version>1.2.3</logback.version>
        <commons.version>2.6</commons.version>
        <jetty.version>9.4.36.v20210114</jetty.version>
        <fastjson.version>1.2.76</fastjson.version>
    </properties>
```
## rpc-proto
- Peer：定义网络传输 地址 & 端口
- ServiceDescriptor：服务的描述，服务即一个对外的方法
- RPC框架中请求与响应之间规定的协议
  - Request：客户端请求ServiceDescriptor，parameters
  - Response：服务器响应code，message，data
## rpc-common
- ReflectUtils：反射工具类
  * @method newInstance 根据clazz创建对象
  * @method getPublicMethods 获取clazz类所有public方法
  * @method invoke 调用指定对象的指定方法，返回调用结果Object
- Junit生成测试类
## rpc-codec
- Encoder & Decoder：【接口】序列化 & 反序列化
- JSONEncoder & JSONDecoder：基于JSON的实现
- Junit生成测试类
## rpc-transport
- TransportClient & TransportServer：【接口】传输客户端 & 传输客户端
- RequestHandler：处理请求的handler，输入数据的通道 & 输出数据的通道
- HttpTransportClient：基于HTTP的客户端
  - 1.创建连接 2.发送请求（获取响应） 3.关闭连接
- HttpTransportServer：基于HTTP的服务端
  - 1.启动并监听端口 2.响应接受请求（进行处理并返回） 3.关闭