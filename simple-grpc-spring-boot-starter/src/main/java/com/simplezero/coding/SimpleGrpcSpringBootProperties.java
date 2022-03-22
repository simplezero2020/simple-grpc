package com.simplezero.coding;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("simplegrpc")
public class SimpleGrpcSpringBootProperties {

    @Value("${spring.application.name}")
    private String applicationName;
    private String address;
    private int port;
    private String datacenter;
    private String segment;
    private EtcdConfig etcd = new EtcdConfig();


    @ConfigurationProperties
    public static class EtcdConfig {

        private List<String> endpoints;

        public List<String> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<String> endpoints) {
            this.endpoints = endpoints;
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public EtcdConfig getEtcd() {
        return etcd;
    }

    public void setEtcd(EtcdConfig etcd) {
        this.etcd = etcd;
    }
}
