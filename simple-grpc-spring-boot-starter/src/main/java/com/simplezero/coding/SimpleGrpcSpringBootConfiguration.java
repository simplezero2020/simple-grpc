package com.simplezero.coding;

import com.simplezero.coding.grpc.core.discovery.ManagedChannelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties({SimpleGrpcSpringBootProperties.class})
@Import({SimpleGrpcServerConfig.class})
public class SimpleGrpcSpringBootConfiguration {

    @Bean
    ManagedChannelFactory managedChannelFactory(@Value("${spring.application.name}") String appName,
                                                SimpleGrpcSpringBootProperties properties,
                                                SimpleGrpcServerConfig config) {
        return new ManagedChannelFactory(appName, config.getSimpleGrpcStorageService(), properties.getDatacenter(),
                properties.getSegment());
    }
}
