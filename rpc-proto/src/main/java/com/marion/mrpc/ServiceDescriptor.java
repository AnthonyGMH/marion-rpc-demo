package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 描述服务：服务即一个对外的方法
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
