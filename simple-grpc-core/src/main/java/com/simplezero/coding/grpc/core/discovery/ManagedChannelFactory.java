package com.simplezero.coding.grpc.core.discovery;

import com.ibm.etcd.client.kv.KvClient;
import com.simplezero.coding.grpc.core.SimpleGrpcStorageService;
import io.grpc.ManagedChannel;
import io.grpc.NameResolverProvider;
import io.grpc.internal.SharedResourceHolder;

import java.util.concurrent.ConcurrentHashMap;


public class ManagedChannelFactory {

    private final String forAppName;
    private final NameResolverProvider nameResolverProvider;
    private final ConcurrentHashMap<String, ManagedChannelResource> serviceResources = new ConcurrentHashMap<>();

    public ManagedChannelFactory(String forAppName, SimpleGrpcStorageService simpleGrpcStorageService, String datacenter, String segments) {
        this.forAppName = forAppName;
        this.nameResolverProvider = new SimpleNameResolverProvider(simpleGrpcStorageService,
                datacenter
                , segments);
    }

    public ManagedChannel create(String serviceName) {
        final ManagedChannelResource channelResource =
                serviceResources.computeIfAbsent(serviceName,
                        (serviceName2) -> new ManagedChannelResource(serviceName2, forAppName,
                                nameResolverProvider));
        return SharedResourceHolder.get(channelResource);
    }

}
