package ru.sbt.cacheProxy.proxy;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CacheProxy {
    //private final CacheType cacheType;
    //private final boolean useZip;
    //private final String fileNamePrefix;
    //private final Class[] identityBy;
    //private final long listList;
    private final URL cacheSource;

    public CacheProxy(URL cacheSource){
        //this.cacheType = CacheType.MEMORY;
        //this.useZip = false;
        //this.fileNamePrefix = "";
        //this.identityBy = null;
        //this.listList = 0;
        this.cacheSource = cacheSource;
    }

    public <T> T cache(Object service) {
        return (T)Proxy.newProxyInstance(service.getClass().getClassLoader(),
                service.getClass().getInterfaces(),
                new CacheHandler(//this.cacheType,
                        //this.useZip,
                        //this.fileNamePrefix,
                        //this.identityBy,
                        //this.listList,
                        this.cacheSource,
                        service));
    }
}
