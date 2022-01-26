package com.marion.mrpc.server;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Request;

/**
 * 调用具体服务
 */
public class ServiceInvoker {

    /**
     * 通过反射工具类ReflectUtils，传入对应的属性作为参数，实现反射调用
     * @param serviceInstance 具体的服务实例对象，利用其属性
     * @param request 具体的请求，利用其属性
     * @return 返回调用据结果 即 Object对象
     */
    public Object invoke(ServiceInstance serviceInstance, Request request) {
        return ReflectUtils.invoke(
            serviceInstance.getTarget(),
            serviceInstance.getMethod(),
            request.getParameters()
        );

    }


}
