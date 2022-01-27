package com.marion.mrpc.codec;

import java.util.IllegalFormatException;

/**
 * 序列化接口
 * 定义encode方法：将传入的任何对象obj转成二进制byte[]数组
 */
public interface Encoder {

    byte[] encode(Object obj);

}
