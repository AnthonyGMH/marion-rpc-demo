# Marion-RPC-demo
从0到1手写一个基于Java的轻量级RPC框架。

## 设计思路 & 模块说明
1. rpc-common：公用类 & 公用方法
2. rpc-codec：编码解码的序列化模块
3. rpc-proto：请求与响应的交互协议模块
4. rpc-transport：负责连接的网络通信模块
5. rpc-server：服务端
6. rpc-client：客户端