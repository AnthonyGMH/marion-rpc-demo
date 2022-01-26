package com.marion.mrpc.example;

import com.marion.mrpc.client.RpcClient;

public class Client {

    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient();
        CalcInterface proxy = rpcClient.getProxy(CalcInterface.class);
        int add = proxy.add(1, 2);
        int minus = proxy.minus(2, 1);
        System.out.println("add="+ add + ", minus=" + minus);
    }

}
