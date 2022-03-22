package com.simplezero.coding.grpc.core.discovery;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.internal.SharedResourceHolder;

import static com.simplezero.coding.grpc.core.common.Constant.DEFAULT_LOAD_BALANCING_POLICY;
import static com.simplezero.coding.grpc.core.common.Constant.SIMPLE_GRPC_URL_PREFIX;

public final class ManagedChannelResource implements SharedResourceHolder.Resource<ManagedChannel> {

    private final String serviceName;
    private final String appName;
    private final NameResolverProvider nameResolverProvider;

    ManagedChannelResource(String serviceName, String appName, NameResolverProvider nameResolverProvider) {
        this.serviceName = serviceName;
        this.appName = appName;
        this.nameResolverProvider = nameResolverProvider;
    }

    @Override
    public ManagedChannel create() {
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forTarget(SIMPLE_GRPC_URL_PREFIX + serviceName)
                .nameResolverFactory(nameResolverProvider)
                .userAgent(appName)
                .defaultLoadBalancingPolicy(DEFAULT_LOAD_BALANCING_POLICY)
                .usePlaintext();
        return channelBuilder.build();
    }

    @Override
    public void close(ManagedChannel instance) {
        if (instance != null && !instance.isShutdown() && !instance.isTerminated()) {
            instance.shutdown();
        }
    }
}
