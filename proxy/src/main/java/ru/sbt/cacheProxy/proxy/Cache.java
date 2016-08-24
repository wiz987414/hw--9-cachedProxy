package ru.sbt.cacheProxy.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {
    CacheType cacheType() default CacheType.MEMORY;
    boolean useZip() default false;
    String fileNamePrefix() default "";
    Class[] identityBy() default {};
    long listList() default 0;
}
