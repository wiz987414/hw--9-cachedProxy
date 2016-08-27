package ru.sbt.cacheProxy.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServiceImpl implements Service {
    @Override
    public double work(String item, Double value, Date date) {
        for (int i = 0; i < 900000000; i++)
            value += 1.5;
        System.out.println(Date.from(Instant.now()));
        return value;
    }

    @Override
    public List<String> work(String item) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 9000000; i++)
            result.add("item");
        System.out.println(Date.from(Instant.now()));
        return result;
    }
}
