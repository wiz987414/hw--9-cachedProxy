package ru.sbt.cacheProxy;

import ru.sbt.cacheProxy.proxy.CacheProxy;
import ru.sbt.cacheProxy.service.Service;
import ru.sbt.cacheProxy.service.ServiceImpl;
import java.io.File;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        Service workService;
        try {
            CacheProxy proxyServer = new CacheProxy(new File("C://Users/UsersPlugins/").toURI().toURL());
            workService = proxyServer.cache(new ServiceImpl());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create valid parent directory with existing path", e);
        }
        System.out.println(workService.work("work1", 11D, Date.from(Instant.now())));
        System.out.println(workService.work("work1", 12D, Date.from(Instant.now())));
        System.out.println(workService.work("work1", 11D, Date.from(Instant.now())));
        System.out.println("\n\n============================\n\n");
        System.out.println(workService.work("work2").size());
        System.out.println(workService.work("work3").size());
        System.out.println(workService.work("work2").size());
    }
}
