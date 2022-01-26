package com.marion.mrpc.example;

import com.marion.mrpc.server.RpcServer;

public class Server {

    public static void main(String[] args) {
        RpcServer server = new RpcServer();
        server.register(CalcInterface.class, new CalcService());
        server.start();
    }

}
