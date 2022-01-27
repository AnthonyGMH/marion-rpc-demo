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
     * 无参构造方法
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
