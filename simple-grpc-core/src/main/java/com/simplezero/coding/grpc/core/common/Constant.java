package com.simplezero.coding.grpc.core.common;

public class Constant {

    public static final String SIMPLE_GRPC_URL_PREFIX = "simplegrpc://";
    public static final String DEFAULT_LOAD_BALANCING_POLICY = "round_robin";
    public static final String ETCD_PATH_SPLITTER = "/";
    public static final String ETCD_SERVICES_INSTANCE_PATH_PREFIX =
            ETCD_PATH_SPLITTER + "simplegrpc" + ETCD_PATH_SPLITTER + "instances";
    public final static int SERVICE_AUTO_REGISTRY_INTERVAL = 300; // s, 5分钟
    public final static int SERVICE_RETRY_REGISTRY_INTERVAL = 3; // s, 3秒
}
