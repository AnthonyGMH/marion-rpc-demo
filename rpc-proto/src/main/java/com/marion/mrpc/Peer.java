package com.marion.mrpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定义网络传输 地址 & 端口
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Peer {

    // IP地址
    private String host;

    // 端口号
    private int port;
}
