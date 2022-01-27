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
     * 获取clazz类的所有public方法, 返回列表Method[]
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
