# Marion-RPC-demo
从0到1手写一个基于Java的轻量级RPC框架。

## 模块说明

1. rpc-common：公用模块 & 反射工具类
2. rpc-codec：负责编码解码的序列化模块
3. rpc-proto：RPC交互过程中约定协议模块
4. rpc-transport：负责管理网络通信的模块
5. rpc-server：RPC服务端模块
6. rpc-client：RPC客户端模块

## 01 | rpc-proto

RPC交互过程中约定协议模块

+   解决如何定位网络通信的对端
+   定义前后端对于服务的统一描述
+   定义RPC中请求与响应的格式规范

### Peer: 定位网络通信的对端

```java
package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定义网络通信 地址 & 端口
 * 作用：定位网络通信的对端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Peer {

    // IP地址
    private String host;

    // 端口号
    private int port;
}

```

### ServiceDescriptor: 服务描述完整代表一个服务

```java
package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 描述服务：服务即一个对外的方法
 * 作用：完整代表一个服务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDescriptor {

    /**
     * 服务的描述：服务即一个对外的方法
     * 1. 这个方法所属对象的类【静态方法属于类】
     * 2. 这个方法的名字
     * 3. 这个方法的参数组合
     * 4. 这个方法的返回值
     */
    private String clazz;
    private String method;
    private String[] parameterTypes;
    private String returnType;

    /**
     * 服务的说明
     * @param clazz 服务对应的类
     * @param method 服务执行的方法
     * @return 将上述4个属性 全都对应设置好的 ServiceDescriptor对象
     */
    public static ServiceDescriptor from(Class clazz, Method method) {
        ServiceDescriptor descriptor = new ServiceDescriptor();
        descriptor.setClazz(clazz.getName());
        descriptor.setMethod(method.getName());
        descriptor.setReturnType(method.getReturnType().getName());

        Class[] parameterClasses = method.getParameterTypes();
        String[] parameterTypes = new String[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            parameterTypes[i] = parameterClasses[i].getName();
        }
        descriptor.setParameterTypes(parameterTypes);
        return descriptor;
    }

    /**
     * 为什么要重写equals方法呢?
     * 我们的RPC框架最终是通过ServiceManager类来管理RPC对外暴露的服务。
     * 而在ServiceManager类当中我们自定义了一个services属性。其实是通过这个属性进行服务管理
     * services属性的数据结构是【Map<ServiceDescriptor, ServiceInstance>】
     * 意思是在services属性中，Map的key是自定义的，key:ServiceDescriptor
     * 管理服务的操作 = 对services属性操作 = 对Map进行操作
     * 而当我们需要用Map.get方法的时候，其实是用到key：ServiceDescriptor的equals方法。
     * 因此我们当然有必要重写这个equals方法，进而重写hashCode和toString。【规定一起重写】
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ServiceDescriptor that = (ServiceDescriptor) obj;
        return this.toString().equals(that.toString());

    }

    @Override
    public int hashCode() {
        int result = Objects.hash(clazz, method, returnType);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        return "ServiceDescriptor{" +
            "clazz='" + clazz + '\'' +
            ", method='" + method + '\'' +
            ", parameterTypes=" + Arrays.toString(parameterTypes) +
            ", returnType='" + returnType + '\'' +
            '}';
    }

}

```

### Request: 作为在RPC中[客户端发出&服务端收到]的处理请求

```java
package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC框架中请求与响应之间规定的协议。
 * 客户端请求ServiceDescriptor，parameters
 * 作用：作为RPC客户端发出的请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request {

    /**
     * @param serviceDescriptor 请求的服务【描述服务：服务即一个对外的方法】
     * @param parameters 请求的参数数组
     */
    private ServiceDescriptor serviceDescriptor;
    private Object[] parameters;
}

```

### Response: 作为在RPC中[服务端发出&客户端收到]的处理响应

```java
package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC框架中请求与响应之间规定的协议。
 * 服务器响应code，message，data
 * 作用: 作为在RPC中[服务端发出&客户端收到]的处理响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    /**
     * @param code 服务器响应返回 0-成功 1-失败， 默认为0-成功
     * @param message 具体的响应返回消息，默认为“ok”，可以用作错误信息
     * @param data 响应返回的数据
     */
    private int code = 0;
    private String message = "ok";
    private Object data;
}

```

## 02 | rpc-common

ReflectUtils反射工具类, 提供RPC中所需的公用方法

-   `newInstance`: 根据clazz创建对象, 返回该对象T
-   `getPublicMethods`: 获取clazz类的所有public方法, 返回列表Method[]
-   `invoke`: 调用指定对象obj的指定方法method, 传入指定参数args, 返回调用结果Object

```java
package com.marion.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 * 作用: 提供RPC中所需的公用方法
 * @method newInstance 根据clazz创建对象
 * @method getPublicMethods 获取clazz类所有public方法
 * @method invoke 调用指定对象的指定方法，返回调用结果
 */
public class ReflectUtils {

    /**
     * 根据clazz创建对象, 返回该对象T
     *
     * @param clazz 待创建对象的类
     * @param <T>   对象类型
     * @return 采用默认无参构造方法，返回创建好的对象实例。
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            // 调用默认的无参构造方法
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    /**
     * 获取clazz类的所有public方法, 返回Method[]
     *
     * @param clazz 待查找的类
     * @return 返回该类所有public方法
     */
    public static Method[] getPublicMethods(Class clazz) {
        // 返回当前类所有的方法，包含public protect private
        Method[] methods = clazz.getDeclaredMethods();
        // 过滤出public方法
        List<Method> objects = new ArrayList<>();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                objects.add(method);
            }
        }
        return objects.toArray(new Method[0]);
    }

    /**
     * 调用指定对象obj的指定方法method, 传入指定参数args, 返回调用结果Object
     * @param obj 指定对象
     * @param method 指定方法
     * @param args 传入的可变参数
     * @return 返回调用结果Object
     */
    public static Object invoke(Object obj, Method method, Object... args) {

        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw new IllegalStateException();
        }

    }

}

```

## 03 | rpc-codec

`序列化`: 将传入的任何对象obj转成二进制数组byte[]

`反序列化`: 将传入的二进制数组byte[]对应转化成clazz类型的对象T，通过泛型可以省去强制转化类这么一个步骤，这样比较方便。

### [interface] Encoder: 序列化接口

```java
package com.marion.mrpc.codec;

import java.util.IllegalFormatException;

/**
 * 序列化接口
 * 定义encode方法：将传入的任何对象obj转成二进制byte[]数组
 */
public interface Encoder {

    byte[] encode(Object obj);

}

```

### [interface] Decoder: 反序列化接口

```java
package com.marion.mrpc.codec;

/**
 * 反序列化接口
 * 定义decode方法：将传入的二进制byte[]数组对应转化成clazz类型的对象T，通过泛型可以省去强制转化类这么一个步骤，这样比较方便。
 */
public interface Decoder {


    <T> T decode(byte[] bytes, Class<T> clazz);

}

```

### JSONEncoder: 基于JSON的序列化实现

```java
package com.marion.mrpc.codec;

import com.alibaba.fastjson.JSON;

/**
 * 基于JSON的序列化实现
 */
public class JSONEncoder implements Encoder{

    @Override
    public byte[] encode(Object obj) {
        return JSON.toJSONBytes(obj);
    }
}

```

### JSONDecoder: 基于JSON的反序列化实现

```java
package com.marion.mrpc.codec;

import com.alibaba.fastjson.JSON;

/**
 * 基于JSON的反序列化实现
 */
public class JSONDecoder implements Decoder {

    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes, clazz);
    }
}

```

## 04 | rpc-transport

负责管理网络通信的模块, 规定采用的传输协议.

封装RpcClient与RpcServer之间的网络连接, 并进行管理.

### [interface] TransportClient: 网络通信客户端接口

1.   `connect`: client->创建连接到对端peer, 即连接server
2.   `write`: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
3.   `close`: 关闭client

```java
package com.marion.mrpc.transport;

import com.marion.mrpc.Peer;

import java.io.InputStream;

/**
 * 网络通信客户端接口
 *      1. connect: client->创建连接到对端peer, 即连接server
 *      2. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
 *      3. close: 关闭client
 */
public interface TransportClient {

    void connect(Peer peer);

    InputStream write(InputStream data);

    void close();
}

```

### [interface] TransportServer: 网络通信服务端接口

1. `init`: 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
2. `start`: 启动jettyServer, 并等待接收请求, 最终[RequestServlet负责]响应进行处理并返回
3. `stop`: 关闭jettyServer

```java
package com.marion.mrpc.transport;

/**
 *  网络通信服务端接口
 *      1. init: 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
 *      2. start: 启动jettyServer, 并等待接收请求, 最终[RequestServlet负责]响应进行处理并返回
 *      3. stop: 关闭jettyServer
 */
public interface TransportServer {


    void init(int port, RequestHandler requestHandler);

    void start();

    void stop();
}

```

### [interface] RequestHandler: RPC服务端对于请求的处理过程

`onRequest`: RPC服务端从[收到请求receive]到[响应返回toResponse]中间的处理过程

```java
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

```

### HttpTransportClient: 基于HTTP连接的网络通信客户端java.net

1.   `connect`: client->创建连接到对端peer, 即连接server
2.   `write`: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
3.   `close`: 关闭client

```java
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

```

### HttpTransportServer: 基于HTTP连接的网络通信服务端jetty

1. `init`: 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
2. `start`: 启动jettyServer, 并等待接收请求, 最终[RequestServlet负责]响应进行处理并返回
3. `stop`: 关闭jettyServer

```java
package com.marion.mrpc.transport;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *  基于HTTP连接的网络通信服务端
 *      1. init: 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
 *      2. start: 启动jettyServer, 并等待接收请求, 最终[RequestServlet负责]响应进行处理并返回
 *      3. stop: 关闭jettyServer
 */
@Slf4j public class HttpTransportServer implements TransportServer {

    private RequestHandler requestHandler;

    private Server jettyServer;

    //  1. 在对应端口port建立jettyServer进行监听, 初始化设置好HttpTransportServer对应的requestHandler
    @Override public void init(int port, RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.jettyServer = new Server(port);

        // 对jettyServer的一些补充处理, 主要是设置Servlet
        ServletContextHandler handler = new ServletContextHandler();
        // ServletHolder是处理网络请求的抽象 // RequestServlet中处理了请求
        ServletHolder servletHolder = new ServletHolder(new RequestServlet());
        handler.addServlet(servletHolder, "/*");
        // 将上述放到jettyServer当中
        jettyServer.setHandler(handler);
    }

    @Override public void start() {
        try {
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
            log.error("server start error, {}, {}", e.getMessage(), e);
        }
    }

    @Override public void stop() {
        try {
            jettyServer.stop();
        } catch (Exception e) {
            log.error("server stop error, {}, {}", e.getMessage(), e);
        }

    }

    /**
     * 对于请求的处理过程
     */
    class RequestServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            // 拿到[接收请求receive]的通道
            InputStream receive = request.getInputStream();
            // 拿到[响应返回toResponse]的通道
            OutputStream toResponse = response.getOutputStream();
            // RPC服务端从[收到请求receive]到[响应返回toResponse]中间的处理过程
            if (requestHandler != null) {
                requestHandler.onRequest(receive, toResponse);
            }
            toResponse.flush();
        }
    }
}

```

## 05 | rpc-server

-   RPC Server的配置
-   统一管理RPC对外提供的服务
  -   对外提供的服务实例
  -   负责对服务进行的调用
-   响应RpcClient的请求并处理返回

### RpcServerConfig: RPC Server的配置

1.   `transportServer`: 负责连接的网络通信模块
2.   `encoder & decoder`: 编码解码的序列化模块
3.   `port端口`：RPC Server启动之后监听什么端口

```java
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

```

### ServiceInstance: 对外提供的服务实例

表示一个具体的服务实例。强调【实例】

1.   `target`: 服务是由哪个对象target提供的
2.   `method`: 具体暴露哪个方法method作为服务

```java
package com.marion.mrpc.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * 对外提供的服务实例
 * 表示一个具体的服务实例。强调【实例】
 *      1. 由哪个对象target提供的
 *      2. 具体暴露哪个方法method作为服务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

    private Object target;

    private Method method;


}

```

### ServiceInvoker: 负责对服务进行调用

`invoke`:

-   通过反射工具类ReflectUtils
-   传入[服务实例serviceInstance] & [协议约定的请求request]作为参数
-   实现反射调用 并 返回结果Object对象

```java
package com.marion.mrpc.server;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Request;

/**
 * 负责对服务进行调用
 * 通过反射工具类ReflectUtils,
 * 传入[服务实例serviceInstance] & [协议约定的请求request],
 * 实现反射调用 并 返回结果Object对象
 */
public class ServiceInvoker {

    /**
     * 通过反射工具类ReflectUtils，传入对应的属性作为参数，实现反射调用
     * @param serviceInstance 具体的服务实例对象，利用其属性
     * @param request 具体的请求，利用其属性
     * @return 返回调用结果 即 Object对象
     */
    public Object invoke(ServiceInstance serviceInstance, Request request) {
        return ReflectUtils.invoke(
            serviceInstance.getTarget(),
            serviceInstance.getMethod(),
            request.getParameters()
        );

    }


}

```

### ServiceManager: 统一管理RPC对外提供的服务

1.   `register`: 根据[Class类]和[该类的具体对象bean], 通过[反射工具类ReflectUtils]得到所有public方法, 并进行服务注册.

2.   `lookup`: 根据[协议约定的请求request]返回对应的[服务实例ServiceInstance]

`services`属性: 已注册的服务列表.

`services`属性采用的数据结构: Map<ServiceDescriptor, ServiceInstance>

```java
package com.marion.mrpc.server;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Request;
import com.marion.mrpc.ServiceDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一管理RPC对外提供的服务
 *      1. register: 根据[Class类]和[该类的具体对象bean], 通过[反射工具类ReflectUtils]得到所有public方法, 并进行服务注册.
 *      2. lookup: 根据[协议约定的请求request]返回对应的[服务实例ServiceInstance]
 *      services属性: 已注册的服务列表.
 *      services属性数据结构: Map<ServiceDescriptor, ServiceInstance>
 */
@Slf4j
public class ServiceManager {
    /**
     * ServiceManager类拥有services属性。
     * 其数据结构是Map<ServiceDescriptor, ServiceInstance>
     * 代表注册服务, < 服务的描述 & 服务具体的实例 >
     */
    private Map<ServiceDescriptor, ServiceInstance> services;
    /**
     * 通过services属性【数据结构：Map】管理服务
     */
    public ServiceManager() {
        this.services = new ConcurrentHashMap<>();
    }

    /**
     * 根据[Class类]和[该类的具体对象bean], 通过反射工具类ReflectUtils得到所有public方法,并进行服务注册.
     * 1. 传入一个Class-interfaceClass和Object-bean，扫描出该Class的所有public方法
     * 2. 针对所有的public方法，结合Class，得到ServiceDescriptor
     * 3. 针对所有的public方法，结合Object，得到ServiceInstance
     * 4. Map<ServiceDescriptor, ServiceInstance>，放到ServiceManager的services属性中。
     * 5. 最终通过services属性【数据结构：Map】管理服务
     * @param interfaceClass 接口类
     * @param bean 实现接口的具体对象，这里采取单例的设计模式
     * @param <T> 泛型
     */
    public <T> void register(Class<T> interfaceClass, T bean) {
        Method[] methods = ReflectUtils.getPublicMethods(interfaceClass);
        for (Method method : methods) {
            // 获取该方法的ServiceDescriptor作为【服务的说明】
            ServiceDescriptor from = ServiceDescriptor.from(interfaceClass, method);
            // 获取该方法的ServiceInstance作为【服务的实例】
            ServiceInstance instance = new ServiceInstance(bean, method);
            // 对应Map上述二者，放入ServiceManager的services属性中。
            services.put(from, instance);
            log.info("[ServiceManager] register, {}, {}", from.getClazz(), from.getMethod());
        }
    }

    /**
     * 根据[协议约定的请求request]返回对应的[服务实例ServiceInstance]
     * @param request RPC框架中请求与响应之间规定的协议。客户端请求ServiceDescriptor，parameters
     * @return 最终从services属性中【Map<ServiceDescriptor, ServiceInstance>】找到该ServiceDescriptor对应的ServiceInstance
     */
    public ServiceInstance lookup(Request request) {
        ServiceDescriptor serviceDescriptor = request.getServiceDescriptor();
        log.info("lookup {}", serviceDescriptor);
        return this.services.get(serviceDescriptor);
    }

}

```

### RpcServer: 响应RpcClient的请求并处理返回

具体流程:

1. 从IO通道中读所有可用的二进制数据, 即获取收到的请求
2. 反序列化得到约定协议格式的请求request
3. 对ServiceManager传入request, 找到对外提供的具体服务实例ServiceInstance
4. ServiceInstance的invoke方法, 通过反射工具类ReflectUtils调用对应的具体方法,
   得到结果invokeResult【Object类-所有可能的数据】
5. 将结果invokeResult写入约定格式的响应response中去, 【还不是二进制数据】最终需要序列化发送回去
   catch: 日志输出异常 并处理
   finally:
6.   将【响应请求并处理返回的】response序列化成二进制数据
7.   往响应返回流中写入二进制数据

```java
package com.marion.mrpc.server;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Request;
import com.marion.mrpc.Response;
import com.marion.mrpc.codec.Decoder;
import com.marion.mrpc.codec.Encoder;
import com.marion.mrpc.transport.RequestHandler;
import com.marion.mrpc.transport.TransportServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * RPC服务端
 */
@Slf4j
public class RpcServer {

    /**
     * 配置信息
     */
    private RpcServerConfig config;             // 配置信息
    private TransportServer transportServer;    // 网络通信模块
    private Encoder encoder;                    // 序列化模块-序列化
    private Decoder decoder;                    // 序列化模块-反序列化
    private ServiceManager serviceManager;      // 服务管理模块
    private ServiceInvoker serviceInvoker;      // 服务调用模块


    // 无参构造方法
    public RpcServer() {
        this(new RpcServerConfig());
    }

    // 有参构造方法, 加载配置类
    public RpcServer(RpcServerConfig config) {
        // 配置
        this.config = config;
        // 网络通信模块 通过反射工具类ReflectUtils 并初始化
        this.transportServer = ReflectUtils.newInstance(config.getTransportServer());
        this.transportServer.init(config.getPort(), this.handler);
        // 序列化模块 通过反射工具类ReflectUtils
        this.encoder = ReflectUtils.newInstance(config.getEncoder());
        this.decoder = ReflectUtils.newInstance(config.getDecoder());
        // 初始化服务
        this.serviceManager = new ServiceManager();
        this.serviceInvoker = new ServiceInvoker();
    }

    // 启动即是网络通信模块启动, 并开启监听
    public void start() {
        this.transportServer.start();
    }

    // 关闭即是网络通信模块关闭, 并关闭监听
    public void stop() {
        this.transportServer.stop();
    }

    // 注册服务 其实就是调用ServiceManager中的register方法 需要什么参数就对应传入
    public <T> void register(Class<T> interfaceClass, T bean) {
        serviceManager.register(interfaceClass, bean);
    }

    /**
     * 处理http请求，加解码
     */
    private RequestHandler handler = new RequestHandler() {

        Response response = new Response();

        /**
         * 1. 从IO通道中读所有可用的二进制数据, 即获取收到的请求
         * 2. 反序列化得到约定协议格式的请求request
         * 3. 对ServiceManager传入request, 找到对外提供的具体服务实例ServiceInstance
         * 4. ServiceInstance的invoke方法, 通过反射工具类ReflectUtils调用对应的具体方法, 得到结果invokeResult【Object类-所有可能的数据】
         * 5. 将结果invokeResult写入约定格式的响应response中去, 【还不是二进制数据】最终需要序列化发送回去
         *      catch: 日志输出异常 并处理
         *      finally:
         *      6. 将【响应请求并处理返回的】response序列化成二进制数据
         *      7. 往响应返回流中写入二进制数据
         * @param receiveRequest 收到的请求
         * @param toResponse 返回的响应
         */


        @Override
        public void onRequest(InputStream receiveRequest, OutputStream toResponse) {
            try {
                // 1. 从IO通道中读所有可用的二进制数据, 即获取收到的请求
                byte[] bytesReceiveRequest = IOUtils.readFully(receiveRequest, receiveRequest.available());
                // 2. 反序列化得到约定协议格式的请求request
                Request request = decoder.decode(bytesReceiveRequest, Request.class);
                log.info("get request, {}", request);
                // 3. 对ServiceManager传入request, 找到对外提供的具体服务实例ServiceInstance
                ServiceInstance serviceInstance = serviceManager.lookup(request);
                log.info("get service, {}", serviceInstance);
                // 4. ServiceInstance的invoke方法, 通过反射工具类ReflectUtils调用对应的具体方法, 得到结果invokeResult【Object类-所有可能的数据】
                Object invokeResult = serviceInvoker.invoke(serviceInstance, request);
                // 5. 将结果invokeResult写入约定格式的响应response中去, 【还不是二进制数据】最终需要序列化发送回去
                response.setData(invokeResult);

            } catch (Exception e) {
                // catch: 日志输出异常 并处理
                log.warn(e.getMessage(), e);
                // 响应中发返回 1-失败码 并返回对应的错误信息
                response.setCode(1);
                response.setMessage("RpcServer get error: " + e.getClass().getName());
            } finally {
                // finally:
                // 二进制数组
                byte[] byteResponse = new byte[0];
                try {
                    // 6. 将【响应请求并处理返回的】response序列化成二进制数据
                    byteResponse = encoder.encode(response);
                    // 7. 往响应返回流中写入二进制数据
                    toResponse.write(byteResponse);
                    log.info("RpcServer response");
                } catch (Exception e) {
                    log.warn("onRequest {}, {}", e.getMessage(), e);
                }
            }
        }
    };


}

```

## 06 | rpc-client

-   RPC Client的配置
-   路由策略的指定
-   自定义动态代理类的处理
-   向RpcServer发送请求并等待响应

### RpcClientConfig: RPC Client的配置

1.   `TransportClient`: 选择网络通信模块，采用什么样的协议
2.   `Encoder & Decoder`: 选择编码解码序列化模块，序列化采用什么格式
3.   `TransportSelector`：选择路由的策略，默认随机策略
4.   `connectCount连接数`: 每个RpcClient与所有RpcServer之间, 默认建立多少连接
5.   `RpcServers`: 初始化默认有的服务器 ip & 端口

```java
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
 *    4. connectCount连接数: 每个rpcClient与所有rpcServer之间, 默认建立多少连接
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
    private List<Peer> servers = Arrays.asList(new Peer("127.0.0.1", 3000));
}

```

### [interface] TransportSelector: 路由策略接口

1. `init`: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
   每个连接需要启动一个本地的transportClient作为网络通信客户端
2. `select`: 从已连接列表中, 按照策略选择一个连接拿来用,
   即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
3. `release`: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
4. `close`: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList

```java
package com.marion.mrpc.client;

import com.marion.mrpc.Peer;
import com.marion.mrpc.transport.TransportClient;

import java.util.List;

/**
 * 路由策略: 随机策略
 *      1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
 *              每个连接需要启动一个本地的transportClient作为网络通信客户端
 *      2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
 *      3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
 *      4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
 */
public interface TransportSelector {

    /**
     * 1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
     *          每个连接需要启动一个本地的transportClient作为网络通信客户端
     *
     * @param rpcServersList  可以连接的rpcServers端点列表信息
     * @param count                每个RpcClient与所有RpcServer之间, 默认建立多少连接
     * @param transportClientClazz transportClient作为网络通信客户端
     */
    void init(List<Peer> rpcServersList,
            int count,
            Class<? extends TransportClient> transportClientClazz);

    /**
     * 2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
     *
     * @return TransportClient
     */
    TransportClient select();

    /**
     * 3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
     *
     * @param transportClient 网络通信客户端
     */
    void release(TransportClient transportClient);

    /**
     * 4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
     */
    void close();

}

```

### RandomTransportSelector: 实现随机策略

1. `init`: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
   每个连接需要启动一个本地的transportClient作为网络通信客户端
2. `select`: 从已连接列表中, 按照策略选择一个连接拿来用,
   即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
3. `release`: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
4. `close`: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList

```java
package com.marion.mrpc.client;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Peer;
import com.marion.mrpc.transport.TransportClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 路由策略: 随机策略
 *      1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
 *              每个连接需要启动一个本地的transportClient作为网络通信客户端
 *      2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
 *      3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
 *      4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
 */
@Slf4j
public class RandomTransportSelector implements TransportSelector {

    /**
     * transportClientsList中存放[已与对端rpcServer建立连接的transportClient]
     */
    private List<TransportClient> transportClientsList;

    public RandomTransportSelector() {
        // 无参构造方法 初始化时[已连接列表]为空
        this.transportClientsList = new ArrayList<>();
    }

    /**
     * 1. init: rpcClient初始化建立连接, 传入rpcServersList可用列表, 一一建立连接,
     *          每个连接需要启动一个本地的transportClient作为网络通信客户端
     *
     * @param rpcServersList  可以连接的rpcServers端点列表信息
     * @param count                每个RpcClient与所有RpcServer之间, 默认建立多少连接
     * @param transportClientClazz transportClient作为网络通信客户端
     */
    @Override
    public synchronized void init(List<Peer> rpcServersList, int count, Class<? extends TransportClient> transportClientClazz) {
        count = Math.max(count, 1);

        for (Peer rpcServer : rpcServersList) {
            // 根据rpcServersList列表创建网络连接, 返回已建立连接的transportClient,
            // 并放入transportClients【List<TransportClient>】
            // 代表已建立连接的transportClient
            for (int i = 0; i < count; i++) {
                TransportClient transportClient = ReflectUtils.newInstance(transportClientClazz);
                // *. connect: client->创建连接到对端peer, 即连接rpcServer
                transportClient.connect(rpcServer);
                transportClientsList.add(transportClient);
                log.info("transportClient {}", transportClient);
            }
        }
    }

    /**
     * 2. select: 从已连接列表中, 按照策略选择一个连接拿来用, 即选择一个TransportClient返回, 同时暂时从transportClientsList中移除
     *
     * @return TransportClient
     */
    @Override
    public synchronized TransportClient select() {
        int i = new Random().nextInt(transportClientsList.size());
        return transportClientsList.remove(i);
    }

    /**
     * 3. release: 对于已经处理完用完的连接进行释放, 即将TransportClient重新加入回transportClientsList中
     *
     * @param transportClient 网络通信客户端
     */
    @Override
    public synchronized void release(TransportClient transportClient) {
        transportClientsList.add(transportClient);
    }

    /**
     * 4. close: 对rpcClient进行关闭, 即销毁所有的已连接网络通信客户端, 即关闭每个transportClient并清理transportClientsList
     */
    @Override
    public synchronized void close() {
        for (TransportClient transportClient : transportClientsList) {
            transportClient.close();
        }
        transportClientsList.clear();
    }
}

```

### RemoteInvoker: 自定义动态代理类的处理

`调用远程服务的前提`: 自定义动态代理类的处理, 必须重写 invoke 方法, 自定义其中的逻辑处理.

重写invoke方法 & 补充invokeRemote私有方法, 最终实现以下处理流程:

1. 构建Request对象

2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法

a. 初始化空的响应response & 初始化空的网络通信客户端client
b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
c. 将request请求序列化成byte[]二进制数组
d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
*. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
+++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
e. 读所有可用的二进制数据
f. 反序列化得到response类的对象
catch: 日志输出异常 并处理
finally: 最后将网络通信客户端transportClient释放
g. 返回对应的响应response

3. 【调用远程服务进行处理后】判断响应, 从响应当中拿到返回的数据

```java
package com.marion.mrpc.client;

import com.marion.mrpc.Request;
import com.marion.mrpc.Response;
import com.marion.mrpc.ServiceDescriptor;
import com.marion.mrpc.codec.*;
import com.marion.mrpc.transport.TransportClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 调用远程服务的前提, 自定义动态代理类的处理.
 * 主要是重写 invoke 方法, 自定义其中的逻辑处理.
 */
@Slf4j
public class RemoteInvoker implements InvocationHandler {
    /**
     * 定义远程服务的所有信息
     */
    private Class clazz;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector selector;

    // 初始化构造方法, 加载远程服务的所有信息
    public <T> RemoteInvoker(Class<T> clazz,Encoder encoder, Decoder decoder, TransportSelector selector) {
        this.clazz = clazz;
        this.encoder = encoder;
        this.decoder = decoder;
        this.selector = selector;
    }

    /**
     * 需要重写自定义的方法
     * @param proxy 动态代理
     * @param method 需要调用的方法
     * @param args 调用方法使用的参数
     * @return Object对象
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        /**
         * 1. 构建Request对象
         * 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
         *          a. 初始化空的响应response & 初始化空的网络通信客户端client
         *          b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
         *          c. 将request请求序列化成byte[]二进制数组
         *          d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
         *               *. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
         *          +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
         *          e. 从IO通道中读所有可用的二进制数据, 即获取返回的响应
         *          f. 反序列化得到response类的对象
         *               catch: 日志输出异常 并处理
         *               finally: 最后将网络通信客户端transportClient释放
         *          g. 返回对应的响应response
         * 3. 【调用远程服务进行处理后】判断响应, 从响应当中拿到返回的数据
         */

        // 1. 构建Request对象
        Request request = new Request();
        request.setServiceDescriptor(ServiceDescriptor.from(clazz, method));
        request.setParameters(args);

        // 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
        Response response = invokeRemote(request);
        // 3. 【调用远程服务进行处理后】判断响应, 从响应当中拿到返回的数据
        // 调用失败
        if (response == null || response.getCode() != 0) {
            throw new IllegalStateException("fail invoke remote " + response);
        }
        // 调用成功
        return response.getData();
    }


    // 2. 通过网络把请求对象发送给Server, 等待Server响应【通过网络传输通信去调用】invokeRemote方法
    private Response invokeRemote(Request request) {

        /**
         *  a. 初始化空的响应response & 初始化空的网络通信客户端client
         *  b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
         *  c. 将request请求序列化成byte[]二进制数组
         *  d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
         *       *. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
         *  +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
         *  e. 从IO通道中读所有可用的二进制数据, 即获取返回的响应
         *  f. 反序列化得到response类的对象
         *  	catch: 日志输出异常 并处理
         *  	finally: 最后将网络通信客户端transportClient释放
         *  g. 返回对应的响应response
         */

        // a. 初始化空的响应response & 初始化空的网络通信客户端client
        Response response = null;
        TransportClient transportClient = null;

        try {
            // b. 通过设置的路由策略selector, 选择一个网络通信客户端transportClient[已连接对端rpcServer]
            transportClient = selector.select();
            // c. 将request请求序列化成byte[]二进制数组
            byte[] byteRequest = encoder.encode(request);
            // d. 并通过网络通信客户端transportClient发送请求, 并获得对应的响应结果afterSendRequest
            //      *. write: client->发送二进制数据data到对端peer, 即发送请求到server, 最终返回得到的InputStream二进制响应信息
            InputStream afterSendRequest = transportClient.write(new ByteArrayInputStream(byteRequest));
            // +++++++++++++++++++++++++++++++++++ >>> 这中间存在一个RpcServer端的处理过程
            // e. 从IO通道中读所有可用的二进制数据, 即获取返回的响应
            byte[] bytes = IOUtils.readFully(afterSendRequest, afterSendRequest.available());
            // f. 反序列化得到response类的对象
            response = decoder.decode(bytes, Response.class);
        } catch (Exception e) {
            // catch: 日志输出异常 并处理
            log.warn("[invokeRemote] e={}, {}", e.getMessage(), e);
            response = new Response();
            response.setCode(1);
            response.setMessage("RpcClient error" + e.getClass() + ":" +e.getMessage());
        } finally {
            // finally: 最后将网络通信客户端transportClient释放
            if (transportClient != null) {
                selector.release(transportClient);
            }
        }
        // g. 返回对应的响应response
        return response;
    }

}

```

### RpcClient: 向RpcServer发送请求并等待响应

`getProxy`: 获取接口的代理对象, 需要new定义一个RemoteInvoke对象传入

`RemoteInvoker`: 调用远程服务的前提, 自定义动态代理类的处理.

实现远程调用!

```java
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

```

