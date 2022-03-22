package com.simplezero.coding;

import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.simplezero.coding.grpc.core.SimpleGrpcStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleGrpcServerConfig {

    @Autowired
    private KvClient kvClient;

    @Autowired
    private SimpleGrpcStorageService simpleGrpcStorageService;

    @Bean
    KvStoreClient client(SimpleGrpcSpringBootProperties properties) {
        SimpleGrpcSpringBootProperties.EtcdConfig etcd = properties.getEtcd();
        return EtcdClient.forEndpoints(etcd.getEndpoints())
                .withPlainText()
                .build();
    }

    @Bean
    KvClient kvClient(KvStoreClient kvStoreClient) {
        return kvStoreClient.getKvClient();
    }


    @Bean
    LeaseClient leaseClient(KvStoreClient kvStoreClient) {
        return kvStoreClient.getLeaseClient();
    }

    @Bean
    SimpleGrpcStorageService simpleGrpcStorageService(KvClient kvClient, LeaseClient leaseClient) {
        return new SimpleGrpcStorageService(kvClient, leaseClient);
    }

    public KvClient getKvClient() {
        return kvClient;
    }

    public SimpleGrpcStorageService getSimpleGrpcStorageService() {
        return simpleGrpcStorageService;
    }
}
