package com.simplezero.coding.grpc.core;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import com.ibm.etcd.client.lease.LeaseClient;
import com.simplezero.coding.grpc.core.common.ServiceInstance;
import com.ypshengxian.ostrich.core.common.InstanceInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.simplezero.coding.grpc.core.common.Constant.ETCD_PATH_SPLITTER;
import static com.simplezero.coding.grpc.core.common.Constant.ETCD_SERVICES_INSTANCE_PATH_PREFIX;

public class SimpleGrpcStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleGrpcStorageService.class);

    private final KvClient kvClient;

    private final LeaseClient leaseClient;

    public SimpleGrpcStorageService(KvClient kvClient, LeaseClient leaseClient) {
        this.kvClient = kvClient;
        this.leaseClient = leaseClient;
    }

    public void register(ServiceInstance instance) {
        final var persistentLease = leaseClient.maintain().start();
        Futures.transform(persistentLease, leaseId -> {
            final var instanceKey = ByteString.copyFromUtf8(
                    Joiner.on(ETCD_PATH_SPLITTER)
                            .join(ETCD_SERVICES_INSTANCE_PATH_PREFIX,
                                    instance.getDatacenter(),
                                    instance.getSegment(),
                                    instance.getDescName(),
                                    instance.getInstanceId()));
            assert leaseId != null;
            logger.info("simplegrpc service {} is registered , address is {} ", instance.getDescName(),
                    instance.getIp() + ":" + instance.getPort());
            return kvClient.put(instanceKey, instance.toByteString(), leaseId).async();
        }, MoreExecutors.directExecutor());
    }

    public List<EquivalentAddressGroup> discovery(String instanceKeyPreFix) {
        final var response = kvClient.get(ByteString.copyFromUtf8(instanceKeyPreFix))
                .asPrefix()
                .sync();
        return response.getKvsList()
                .stream()
                .map(kv -> {
                    InstanceInfo instance;
                    try {
                        instance = InstanceInfo.parseFrom(kv.getValue());
                    } catch (InvalidProtocolBufferException e) {
                        return null;
                    }
                    logger.info("simplegrpc service {} discovery by {}:{} ",
                            instance.getDescName(), instance.getIp(), instance.getPort());
                    return new EquivalentAddressGroup(new InetSocketAddress(
                            instance.getIp(), instance.getPort()));
                }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public KvClient.Watch watch(String instanceKeyPrefix, StreamObserver<WatchUpdate> updateObserver) {
        return kvClient.watch(ByteString.copyFromUtf8(instanceKeyPrefix))
                .asPrefix()
                .start(updateObserver);
    }

}
