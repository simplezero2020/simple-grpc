package com.simplezero.coding.grpc.core.discovery;

import com.ibm.etcd.client.kv.KvClient;
import com.simplezero.coding.grpc.core.SimpleGrpcStorageService;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.internal.GrpcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * usage: ostrich://{service-name}/?{attr-key1}={attr-value1}&amp;{attr-key2}={attr-value2}
 */
public class SimpleNameResolverProvider extends NameResolverProvider {

    public static final String SCHEME = "simplegrpc";

    public static final Logger logger = LoggerFactory.getLogger(SimpleNameResolverProvider.class);

    private final String segment;

    private final String datacenter;

    private final SimpleGrpcStorageService simpleGrpcStorageService;


    public SimpleNameResolverProvider(SimpleGrpcStorageService simpleGrpcStorageService, String segment,
                                      String datacenter) {
        this.simpleGrpcStorageService = simpleGrpcStorageService;
        this.segment = segment;
        this.datacenter = datacenter;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, final NameResolver.Args args) {
        if (!SCHEME.equals(targetUri.getScheme())) {
            return null;
        }
        final String authority = requireNonNull(targetUri.getAuthority(), "authority");
        final String query = targetUri.getQuery() == null ? "" : targetUri.getQuery();
        return new SimpleNameResolver(authority, query, simpleGrpcStorageService,
                datacenter, segment);
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

}
