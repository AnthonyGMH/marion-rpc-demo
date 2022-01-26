package com.marion.mrpc.example;

public class CalcService implements CalcInterface{
    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int minus(int a, int b) {
        return a - b;
    }
}
