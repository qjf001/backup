package com.qjf.backup.util;

public class HostAndPort {
    private String host;
    private int port;

    public HostAndPort(String remoteHost, int defaultPort) {
        String host = remoteHost;
        if (remoteHost.contains(":")) {
            String[] data = remoteHost.split(":");
            host = data[0];
            defaultPort = Integer.parseInt(data[1]);
        }
        this.host = host;
        this.port = defaultPort;
    }

    public HostAndPort() {

    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
