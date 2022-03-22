package com.simplezero.coding.example.client;

import com.simplezero.coding.annotations.EnableSimpleGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableSimpleGrpc
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@SpringBootApplication(scanBasePackages = {"com.simplezero.coding.example.client"})
public class ExampleClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleClientApplication.class, args);
    }

}
