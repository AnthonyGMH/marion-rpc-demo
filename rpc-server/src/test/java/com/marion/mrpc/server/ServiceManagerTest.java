package com.marion.mrpc.server;

import com.marion.common.utils.ReflectUtils;
import com.marion.mrpc.Request;
import com.marion.mrpc.ServiceDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ServiceManagerTest {

    ServiceManager serviceManager;

    @Before
    public void init() {
        serviceManager = new ServiceManager();

//        测试lookup需要在初始化先注册服务。测试register则注释掉下面两行代码
        TestInterface bean = new TestClass();
        serviceManager.register(TestInterface.class, bean);

    }

    @Test public void register() {
        TestInterface bean = new TestClass();
        serviceManager.register(TestInterface.class, bean);
    }

    @Test public void lookup() {
        Method method = ReflectUtils.getPublicMethods(TestInterface.class)[0];
        ServiceDescriptor from = ServiceDescriptor.from(TestInterface.class, method);

        Request request = new Request();
        request.setServiceDescriptor(from);

        ServiceInstance lookup = serviceManager.lookup(request);
        assertNotNull(lookup);
    }
}