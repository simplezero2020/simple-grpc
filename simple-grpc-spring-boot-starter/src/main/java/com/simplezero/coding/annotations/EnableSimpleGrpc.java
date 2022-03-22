package com.simplezero.coding.annotations;


import com.simplezero.coding.SimpleGrpcSpringBootConfiguration;
import com.simplezero.coding.server.SpringBootServerRunner;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        SimpleGrpcSpringBootConfiguration.class,
        SpringBootServerRunner.class,
})
public @interface EnableSimpleGrpc {

}
