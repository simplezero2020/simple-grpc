package com.simplezero.coding.server;

import com.alibaba.fastjson.JSON;
import com.simplezero.coding.SimpleGrpcSpringBootProperties;
import com.simplezero.coding.annotations.GrpcService;
import com.simplezero.coding.grpc.core.SimpleGrpcStorageService;
import com.simplezero.coding.grpc.core.common.Constant;
import com.simplezero.coding.grpc.core.common.ServiceInstance;
import com.ypshengxian.ostrich.core.common.InstanceInfo;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.LogExceptionRunnable;
import io.grpc.internal.SharedResourceHolder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;

public class SpringBootServerRunner implements CommandLineRunner,
        ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootServerRunner.class);

    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Autowired
    private SimpleGrpcStorageService storageService;
    @Autowired
    private SimpleGrpcSpringBootProperties simpleGrpcSpringBootProperties;
    private final List<ServerServiceDefinition> scannedServices = newArrayList();
    private InstanceInfo instanceInfo;
    private Server server;
    private ServerBuilder<?> serverBuilder;
    private final HealthStatusManager healthStatusManager = new HealthStatusManager();
    private ScheduledFuture<?> serviceRegisterTask;
    private ScheduledExecutorService scheduledExecutor = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
    private Executor executor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
    private volatile boolean running = true;

    @Override
    public void run(String... args) throws Exception {
        logger.info("simplegrpc startup ... , params is {}", JSON.toJSONString(simpleGrpcSpringBootProperties));
        addServices();
        startServer();
        registerServices();
        logger.info("simplegrpc already started ,listening on ip {} port {}.",
                simpleGrpcSpringBootProperties.getAddress(), simpleGrpcSpringBootProperties.getPort());
    }

    private void addServices() {
        serverBuilder = ServerBuilder.forPort(simpleGrpcSpringBootProperties.getPort());
        serverBuilder.addService(ProtoReflectionService.newInstance());
        doAddServices(serverBuilder);
    }

    private void doAddServices(ServerBuilder<?> serverBuilder) {
        final Class<? extends Annotation> annotationType = GrpcService.class;
        Arrays.stream(applicationContext.getBeanNamesForType(BindableService.class)).forEach(name -> {
            boolean register = false;
            final Map<String, Object> beansWithAnnotation = applicationContext
                    .getBeansWithAnnotation(annotationType);
            final BeanDefinition beanDefinition = applicationContext.getBeanFactory()
                    .getBeanDefinition(name);
            if (beansWithAnnotation.containsKey(name)) {
                register = true;
            } else if (beanDefinition.getSource() instanceof AnnotatedTypeMetadata) {
                register = ((AnnotatedTypeMetadata) beanDefinition.getSource())
                        .isAnnotated(annotationType.getName());
            }
            if (register) {
                BindableService srv = applicationContext.getBeanFactory()
                        .getBean(name, BindableService.class);
                ServerServiceDefinition serviceDefinition = srv.bindService();
                GrpcService serviceAnn = applicationContext
                        .findAnnotationOnBean(name, GrpcService.class);
                if (serviceAnn != null) {
                    serverBuilder.addService(serviceDefinition);
                    scannedServices.add(serviceDefinition);
                }
            }
        });
    }

    public void registerServices() {
        instanceInfo = InstanceInfo.newBuilder()
                .setIp(simpleGrpcSpringBootProperties.getAddress())
                .setPort(simpleGrpcSpringBootProperties.getPort())
                .setAppName(simpleGrpcSpringBootProperties.getApplicationName())
                .setDatacenter(simpleGrpcSpringBootProperties.getDatacenter())
                .setSegment(simpleGrpcSpringBootProperties.getSegment())
                .build();
        startServiceRegister(0);
    }

    private void startServiceRegister(int interval) {
        if (serviceRegisterTask != null && !serviceRegisterTask.isCancelled()
                && !serviceRegisterTask.isDone()) {
            serviceRegisterTask.cancel(true);
        }
        serviceRegisterTask = scheduledExecutor.schedule(
                () -> executor.execute(new LogExceptionRunnable(new ServiceRegisterJob())),
                interval, TimeUnit.SECONDS);
    }

    private class ServiceRegisterJob implements Runnable {
        @Override
        public void run() {
            try {
                scannedServices.forEach(serviceDefinition ->
                        registerService(serviceDefinition.getServiceDescriptor()));
            } catch (Exception exception) {
                startServiceRegister(Constant.SERVICE_RETRY_REGISTRY_INTERVAL);
            }
        }

        private void registerService(@Nonnull final ServiceDescriptor serviceDescriptor) {
            final String serviceName = serviceDescriptor.getName();
            healthStatusManager.setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING);
            final var serviceInstance = new ServiceInstance(InstanceInfo.newBuilder()
                    .setAppName(instanceInfo.getAppName())
                    .setDatacenter(instanceInfo.getDatacenter())
                    .setSegment(instanceInfo.getSegment())
                    .setDescName(serviceDescriptor.getName())
                    .setIp(instanceInfo.getIp())
                    .setPort(instanceInfo.getPort())
                    .setHostname(instanceInfo.getHostname())
                    .build());
            storageService.register(serviceInstance);
        }
    }


    private void startServer() throws IOException {
        server = serverBuilder.build().start();
        final Thread awaitThread = new Thread(() -> {
            try {
                server.awaitTermination();
                logger.info("SimpleGrpc server stopped.");
            } catch (InterruptedException e) {
                logger.error("SimpleGrpc stopped.", e);
                Thread.currentThread().interrupt();
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        if (!running) {
            return;
        }
        running = false;
        unregisterServices();
        if (server != null) {
            server.shutdown();
        }
        if (scheduledExecutor != null) {
            scheduledExecutor = SharedResourceHolder
                    .release(GrpcUtil.TIMER_SERVICE, scheduledExecutor);
        }
        if (executor != null) {
            executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
        }
        logger.info("SimpleGrpc server stopped");
    }

    private void unregisterServices() {
        if (server == null) {
            return;
        }
        scannedServices.forEach(
                serviceDefinition ->
                        healthStatusManager.clearStatus(serviceDefinition.getServiceDescriptor().getName()));
    }
}
