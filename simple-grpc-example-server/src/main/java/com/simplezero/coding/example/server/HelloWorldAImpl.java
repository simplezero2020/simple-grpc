package com.simplezero.coding.example.server;

import com.simplezero.coding.annotations.SimpleGrpcService;
import com.simplezero.coding.example.helloworldA.HelloReply;
import com.simplezero.coding.example.helloworldA.HelloRequest;
import com.simplezero.coding.example.helloworldA.HelloWorldAGrpc;
import org.springframework.stereotype.Component;


@Component
@SimpleGrpcService(HelloWorldAGrpc.HelloWorldAImplBase.class)
public class HelloWorldAImpl {

    public HelloReply sayHello(HelloRequest req) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello "+req.getName()+" ,I am A ").build();
        return reply;
    }
}
