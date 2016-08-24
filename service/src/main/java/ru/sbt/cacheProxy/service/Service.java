package ru.sbt.cacheProxy.service;

import ru.sbt.cacheProxy.proxy.Cache;
import ru.sbt.cacheProxy.proxy.CacheType;

import java.util.Date;
import java.util.List;

public interface Service {
    @Cache(cacheType = CacheType.FILE, fileNamePrefix = "data", useZip = true, identityBy = {String.class, Double.class})
    double work(String item, Double value, Date date);

    @Cache(cacheType = CacheType.MEMORY, listList = 100_000)
    List<String> work(String item);
}
