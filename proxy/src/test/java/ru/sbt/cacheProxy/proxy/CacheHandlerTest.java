package ru.sbt.cacheProxy.proxy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CacheHandlerTest {
    private Method testWorkMethod;
    private Object[] testArgs;
    private Class[] testIdentity;
    private List<Object> testCacheArgs;
    @Mock
    private CacheHandler testImpl;

    @Before
    public void setUp() throws Exception {
        testImpl = new CacheHandler(new File("C://Users/UsersPlugins/").toURI().toURL(), this);
        testWorkMethod = this.getClass().getMethod("setUp");
        testArgs = new Object[]{"work1", 11D, Date.from(Instant.now())};
        testIdentity = new Class[]{String.class, Double.class};
        testCacheArgs = new ArrayList<>();
        testCacheArgs.add(testWorkMethod);
        testCacheArgs.addAll(asList("work1", 11D));
    }

/*    @Test
    public void identityCheckTestWithMock() {
        testImpl = mock(CacheHandler.class);
        Map<List<Object>,Object> testCacheMap = new HashMap<>();
        testCacheMap.put(testCacheArgs, 1000);
        doReturn(testCacheMap).when(testImpl).getMemoryCache();
        //when(testImpl.getMemoryCache()).thenReturn(testCacheMap);
        //when(testImpl.isIdentity(testArgs, asList(testWorkMethod, "work1", 11D), testIdentity)).thenReturn(true);
        doReturn(true).when(testImpl).isIdentity(testArgs, asList(testWorkMethod, "work1", 11D), testIdentity);
        assertEquals(true, testImpl.identityCheck(testWorkMethod, testArgs, testIdentity));
        //assertEquals(testCacheArgs, testImpl.key(testWorkMethod,testArgs));
    }*/

    @Test
    public void identityCheckTest() {
        testImpl.updateCache(testCacheArgs, 1000);
        assertEquals(true, testImpl.identityCheck(testWorkMethod, testArgs, testIdentity));
    }

    @Test
    public void identityCheckInverseTest() {
        testImpl.updateCache(asList("work2", 11D), 1000);
        assertEquals(false, testImpl.identityCheck(testWorkMethod, testArgs, testIdentity));
    }

    @Test
    public void isIdentityTest() {
        assertEquals(true, testImpl.isIdentity(testArgs, testCacheArgs, testIdentity));
    }

    @Test
    public void isIdentityInverseTest() {
        assertEquals(false, testImpl.isIdentity(new Object[]{12, 11L, Date.from(Instant.now())}, testCacheArgs, testIdentity));
    }


}