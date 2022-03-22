package com.simplezero.coding.annotations;

import io.grpc.ServerInterceptor;
import org.springframework.stereotype.Service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GrpcService {

    /**
     * gRPC的interceptor编写复杂度比较高, 暂时不开放此功能
     */
    @Deprecated
    Class<? extends ServerInterceptor>[] interceptors() default {};

    /**
     * gRPC的interceptor编写复杂度比较高, 暂时不开放此功能
     */
    @Deprecated
    boolean applyGlobalInterceptors() default true;
}
