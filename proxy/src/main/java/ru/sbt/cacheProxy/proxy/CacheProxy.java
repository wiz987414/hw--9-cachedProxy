package ru.sbt.cacheProxy.proxy;

import java.lang.reflect.Proxy;
import java.net.URL;

public class CacheProxy {
    private final URL cacheSource;

    public CacheProxy(URL cacheSource) {
        this.cacheSource = cacheSource;
    }

    public <T> T cache(Object service) {
        return (T) Proxy.newProxyInstance(service.getClass().getClassLoader(),
                service.getClass().getInterfaces(),
                new CacheHandler(this.cacheSource, service));
    }
}
