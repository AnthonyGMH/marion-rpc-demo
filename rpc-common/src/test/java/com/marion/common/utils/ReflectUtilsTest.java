package com.marion.common.utils;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ReflectUtilsTest {

    @Test public void newInstance() {
        TestClass testClass = ReflectUtils.newInstance(TestClass.class);
        // 是否创建成功
        assertNotNull(testClass);
    }

    @Test public void getPublicMethods() {
        Method[] publicMethods = ReflectUtils.getPublicMethods(TestClass.class);
        for (Method publicMethod : publicMethods) {
            System.out.println(publicMethod.getName());
        }
        // TestClass只有一个public方法, 对此进行判断.
        assertEquals(1, publicMethods.length);
        assertEquals("b", publicMethods[0].getName());
    }

    @Test public void invoke() {
        TestClass testClass = new TestClass();
        Method[] publicMethods = ReflectUtils.getPublicMethods(TestClass.class);
        Object o = ReflectUtils.invoke(testClass, publicMethods[0]);
        // 判断是否能成功调用指定对象的指定方法
        assertEquals("b", o);
    }
}