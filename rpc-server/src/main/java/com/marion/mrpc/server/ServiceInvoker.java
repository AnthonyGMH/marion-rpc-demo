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
