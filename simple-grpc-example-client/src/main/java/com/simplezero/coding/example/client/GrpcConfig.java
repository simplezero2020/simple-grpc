package com.simplezero.coding.example.client;

import com.simplezero.coding.example.helloworldC.HelloWorldCGrpc;
import com.simplezero.coding.grpc.core.discovery.ManagedChannelFactory;
import com.simplezero.coding.example.helloworldA.HelloWorldAGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * grpc-bean配置
 */
@Configuration
public class GrpcConfig {

    @Bean
    public HelloWorldAGrpc.HelloWorldABlockingStub helloWorldABlockingStub(ManagedChannelFactory factory) {
        return HelloWorldAGrpc.newBlockingStub(factory.create(HelloWorldAGrpc.SERVICE_NAME));
    }

    @Bean
    public HelloWorldCGrpc.HelloWorldCBlockingStub helloWorldCBlockingStub(ManagedChannelFactory factory) {
        return HelloWorldCGrpc.newBlockingStub(factory.create(HelloWorldCGrpc.SERVICE_NAME));
    }

}


