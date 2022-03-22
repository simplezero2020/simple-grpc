package com.simplezero.coding.example.server;

import com.simplezero.coding.annotations.SimpleGrpcService;
import com.simplezero.coding.example.helloworldC.HelloReply;
import com.simplezero.coding.example.helloworldC.HelloRequest;
import com.simplezero.coding.example.helloworldC.HelloWorldCGrpc;
import org.springframework.stereotype.Component;


@Component
@SimpleGrpcService(HelloWorldCGrpc.HelloWorldCImplBase.class)
public class HelloWorldCImpl {

    public HelloReply sayHello(HelloRequest req) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello "+req.getName()+" , I am C ").build();
        return reply;
    }
}
