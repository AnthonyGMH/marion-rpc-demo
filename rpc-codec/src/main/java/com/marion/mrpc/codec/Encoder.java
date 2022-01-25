package com.marion.mrpc.codec;

import java.util.IllegalFormatException;

/**
 * 序列化接口
 * 定义encode方法：将任何对象转成Byte[]数组
 */
public interface Encoder {

    byte[] encode(Object obj);

}
