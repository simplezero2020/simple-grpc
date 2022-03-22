package com.simplezero.coding.example.server;

import com.simplezero.coding.annotations.EnableSimpleGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableSimpleGrpc
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@SpringBootApplication(scanBasePackages = {"com.simplezero.coding.example.server"})
public class ExampleServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleServerApplication.class, args);
    }

}
