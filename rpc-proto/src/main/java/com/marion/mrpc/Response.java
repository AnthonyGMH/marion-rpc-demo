package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC框架中请求与响应之间规定的协议。
 * 服务器响应code，message，data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    /**
     * @param code 服务器响应返回 0-成功 1-失败， 默认为0-成功
     * @param message 具体的响应返回消息，默认为“ok”，可以用作错误信息
     * @param data 响应返回的数据
     */
    private int code = 0;
    private String message = "ok";
    private Object data;
}
