package com.simplezero.coding.grpc.core.discovery;

import com.google.common.base.Joiner;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import com.simplezero.coding.grpc.core.SimpleGrpcStorageService;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.LogExceptionRunnable;
import io.grpc.internal.SharedResourceHolder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

import static com.simplezero.coding.grpc.core.common.Constant.ETCD_PATH_SPLITTER;
import static com.simplezero.coding.grpc.core.common.Constant.ETCD_SERVICES_INSTANCE_PATH_PREFIX;



public class SimpleNameResolver extends NameResolver {

    private static final Logger logger = LoggerFactory.getLogger(SimpleNameResolver.class);
    private Listener listener;
    private String authority;
    private SimpleGrpcStorageService simpleGrpcStorageService;
    private final String instancesPrefix;
    private final Executor channelExecutor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);

    public SimpleNameResolver(@Nonnull String authority, String query,
                              SimpleGrpcStorageService simpleGrpcStorageService,
                              String segment, String datacenter) {
        this.authority =
                (query != null && !query.isEmpty()) ? authority + "?" + query : authority;
        this.simpleGrpcStorageService = simpleGrpcStorageService;
        this.instancesPrefix =
                Joiner.on(ETCD_PATH_SPLITTER)
                        .join(ETCD_SERVICES_INSTANCE_PATH_PREFIX,
                                datacenter,
                                segment);
    }

    @Override
    public synchronized void start(Listener listener) {
        this.listener = listener;
        resolve();
        watch();
    }


    @Override
    public String getServiceAuthority() {
        return authority;
    }

    @Override
    public void shutdown() {
    }

    private KvClient.Watch watcher;

    private void resolve() {
        listener.onAddresses(simpleGrpcStorageService.discovery(instancesPrefix + "/" + authority), Attributes.EMPTY);
    }

    private void watch() {
        this.channelExecutor.execute(
                new LogExceptionRunnable(() -> {
                    watcher = simpleGrpcStorageService.watch(instancesPrefix + "/" + authority, new WatchObserver());
                }));
    }


    private class WatchObserver implements StreamObserver<WatchUpdate> {
        @Override
        public void onNext(WatchUpdate watchUpdate) {
            for (Event event : watchUpdate.getEvents()) {
                logger.info("etcd event happend, type " + event.getType() + ", kv " + event.getKv());
                resolve();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            watchAgain();

        }

        @Override
        public void onCompleted() {
            watchAgain();
        }

        private void watchAgain() {
            if (watcher != null) {
                watcher.close();
                watcher = null;
            }
            watch();
        }
    }

}
