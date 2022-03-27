package com.simplezero.coding.example.client;

import com.simplezero.coding.annotations.SimpleGrpcService;
import com.simplezero.coding.example.helloworldA.HelloWorldAGrpc;
import com.simplezero.coding.example.helloworldB.HelloReply;
import com.simplezero.coding.example.helloworldB.HelloRequest;
import com.simplezero.coding.example.helloworldB.HelloWorldBGrpc;
import com.simplezero.coding.example.helloworldC.HelloWorldCGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@SimpleGrpcService(HelloWorldBGrpc.HelloWorldBImplBase.class)
public class HelloWorldBImpl {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldBImpl.class);

    @Autowired
    HelloWorldAGrpc.HelloWorldABlockingStub helloWorldABlockingStub;


    @Autowired
    HelloWorldCGrpc.HelloWorldCBlockingStub helloWorldCBlockingStub;


    public HelloReply sayHello(HelloRequest req) {
        com.simplezero.coding.example.helloworldA.HelloReply replyA =
                helloWorldABlockingStub.sayHello(com.simplezero.coding.example.helloworldA.HelloRequest.newBuilder().setName("B").build());
        logger.info("client invoke server HelloWorldAImpl#sayHello,replyA:{}", replyA.getMessage());
        com.simplezero.coding.example.helloworldC.HelloReply replyC =
                helloWorldCBlockingStub.sayHello(com.simplezero.coding.example.helloworldC.HelloRequest.newBuilder().setName("B").build());
        logger.info("client invoke server HelloWorldCImpl#sayHello,replyC:{}", replyC.getMessage());
        return HelloReply.newBuilder().setMessage(replyA.getMessage() + ";" + replyC.getMessage()).build();
    }
}