package com.marion.mrpc.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * 表示一个具体的服务实例。强调【实例】
 *      1. 由哪个对象Object提供的
 *      2. 具体暴露哪个方法Method作为服务
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

    private Object target;

    private Method method;


}
