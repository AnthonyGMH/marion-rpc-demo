package com.marion.mrpc.codec;

/**
 * 反序列化接口
 * 定义decode方法：将传入的二进制byte[]数组对应转化成clazz类型的对象T，通过泛型可以省去强制转化类这么一个步骤，这样比较方便。
 */
public interface Decoder {


    <T> T decode(byte[] bytes, Class<T> clazz);

}
