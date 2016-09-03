package ru.sbt.cacheProxy.proxy;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.util.Arrays.asList;

class CacheHandler implements InvocationHandler {
    private final URL cacheSource;
    private final Object delegate;
    private final Map<List<Object>, Object> memoryCache;

    CacheHandler(URL cacheSource,
                 Object delegate) {
        this.cacheSource = cacheSource;
        this.delegate = delegate;
        this.memoryCache = new HashMap<>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        if (!method.isAnnotationPresent(Cache.class)) return invoke(method, args);
        Cache checkParams = method.getAnnotation(Cache.class);
        if (memoryCache.keySet().isEmpty()) {
            return cacheUpdate(method, args);
        }
        if (asList(checkParams.identityBy()).isEmpty()) {
            return cacheUpdate(method, args);
        } else {
            if (identityCheck(method, args, checkParams.identityBy())) {
                result = memoryCache.get(getCached(args, checkParams.identityBy()));
            } else result = cacheUpdate(method, args);
        }
        return result;
    }

    private Object cacheUpdate(Method method, Object[] args) throws Throwable {
        Cache updateConfig = method.getAnnotation(Cache.class);
        Object result = null;
        if (updateConfig.cacheType() == CacheType.FILE) {
            List<File> cacheFilesList = asList(new File(this.cacheSource.toURI()).listFiles());
            String fileName = getCacheFileName(method.getName(), updateConfig.fileNamePrefix(), args, updateConfig.identityBy());
            if (cacheFilesList.isEmpty()) {
                return invokeCache(method, args, updateConfig.listList(), fileName);
            }
            for (File file : cacheFilesList) {
                if (fileName.substring(0, fileName.length() - 4).concat(".zip").endsWith(file.getName())) {
                    return deserialise(method, fileName.substring(0, fileName.length() - 4).concat(".zip"));
                }
                if (fileName.endsWith(file.getName())) {
                    return deserialise(method, fileName);
                }
            }
            result = invokeCache(method, args, updateConfig.listList(), fileName);
        } else if (!memoryCache.containsKey(key(method, args))) {
            result = invoke(method, args);
            if (method.getReturnType() == List.class && updateConfig.listList() > 0)
                memoryCache.put(key(method, args), cachedList((List<Object>) result, updateConfig.listList()));
            else memoryCache.put(key(method, args), result);
        }
        if (memoryCache.containsKey(key(method, args)))
            result = memoryCache.get(key(method, args));
        return result;
    }

    boolean identityCheck(Method method, Object[] args, Class[] identityList) {
        for (List<Object> checkKey : getMemoryCache().keySet()) {
            List<Class> itemClasses = new ArrayList<>();
            for (Object listItem : checkKey) {
                itemClasses.add(listItem.getClass());
            }
            if (itemClasses.containsAll(asList(identityList)) &&
                    (checkKey.contains(method)) && isIdentity(args, checkKey, identityList)) {
                return true;
            }
        }
        return false;
    }

    private Object invoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to get access to target method", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to invoke method", e);
        }
    }

    boolean isIdentity(Object[] args, List<Object> cacheArgs, Class[] identityList) {
        boolean checkStatus = false;
        for (Object checkArg : args) {
            if (asList(identityList).contains(checkArg.getClass()) && cacheArgs.contains(checkArg))
                checkStatus = true;
            else if (asList(identityList).contains(checkArg.getClass()) && !cacheArgs.contains(checkArg))
                checkStatus = false;
        }
        return checkStatus;
    }

    private List<Object> getCached(Object[] args, Class[] identityList) {
        boolean checkStatus = false;
        List<Object> result = null;
        for (List<Object> checkKey : memoryCache.keySet()) {
            List<Class> itemClasses = checkKey.stream().map((Function<Object,
                    Class<?>>) Object::getClass).collect(Collectors.toList());
            if (itemClasses.containsAll(asList(identityList))) {
                checkStatus = isIdentity(args, checkKey, identityList);
            }
            if (checkStatus)
                result = checkKey;
        }
        return result;
    }

    List<Object> key(Method method, Object[] args) {
        List<Object> key = new ArrayList<>();
        key.add(method);
        key.addAll(asList(args));
        return key;
    }

    private String getCacheFileName(String methodName, String namePrefix, Object[] args, Class[] identityList) {
        StringBuilder nameString = new StringBuilder();
        nameString.append(this.cacheSource.getFile());
        if (!Objects.equals(namePrefix, ""))
            nameString.append(namePrefix);
        else nameString.append(methodName);
        for (Object arg : args) {
            if (asList(identityList).isEmpty()) {
                nameString.append("-");
                nameString.append(arg);
            } else if (asList(identityList).contains(arg.getClass())) {
                nameString.append("-");
                nameString.append(arg);
            }
        }
        nameString.append(".cer");
        return nameString.toString();
    }

    private Object invokeCache(Method method, Object[] args, long cacheListSize, String fileName) throws Throwable {
        Object result = invoke(method, args);
        if (method.getReturnType() == List.class && cacheListSize > 0)
            serialise(method, cachedList((List<Object>) result, cacheListSize), fileName);
        else serialise(method, result, fileName);
        return result;
    }

    private void serialise(Method method, Object storedObject, String fileName) {
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(fileName))) {
            File newFile = new File(fileName);
            if (newFile.createNewFile())
                throw new RuntimeException("Unable to create new file");
            stream.writeObject(storedObject);
            if (method.getAnnotation(Cache.class).useZip()) {
                zipFile(fileName);
                newFile.deleteOnExit();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to create file in target destination, check path parameters", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write data in source file", e);
        }
    }

    private void zipFile(String fileName) {
        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(fileName.
                substring(0, fileName.length() - 4) + ".zip"));
             FileInputStream fileStream = new FileInputStream(fileName)) {
            ZipEntry entry1 = new ZipEntry(fileName.substring(this.cacheSource.getPath().length()));
            zipStream.putNextEntry(entry1);
            byte[] buffer = new byte[fileStream.available()];
            fileStream.read(buffer);
            zipStream.write(buffer);
            zipStream.closeEntry();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cache file not exist, need to check source path, or file name", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load data from source file", e);
        }
    }

    private void unzipFile(String fileName) throws IOException {
        ZipFile zipFile = new ZipFile(fileName);
        Enumeration files = zipFile.entries();
        File file;
        while (files.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) files.nextElement();
            InputStream zipFileInputStream = zipFile.getInputStream(entry);
            byte[] buffer = new byte[1024];
            int bytesRead;
            file = new File(new File(this.cacheSource.getPath()).getAbsolutePath() + File.separator + entry.getName());
            file.createNewFile();
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                while ((bytesRead = zipFileInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                zipFileInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to load data from source file", e);
            }
        }
    }

    private Object deserialise(Method method, String fileName) {
        String readFile = fileName.substring(0, fileName.length() - 4).concat(".cer");
        Object generatedObject;
        if (method.getAnnotation(Cache.class).useZip()) {
            if (fileName.endsWith(".zip"))
                try {
                    unzipFile(fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load data from source file", e);
                }
        }
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(readFile))) {
            generatedObject = stream.readObject();
            if (method.getAnnotation(Cache.class).useZip()) {
                File removeFile = new File(readFile);
                removeFile.deleteOnExit();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cache file not exist, need to check source path, or file name", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load data from source file", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Deserialising object not compatible. Need to check class version", e);
        }

        return generatedObject;
    }

    private List<Object> cachedList(List<Object> baseList, long cacheSize) {
        return baseList.subList(0, (int) cacheSize);
    }

    Map<List<Object>, Object> getMemoryCache() {
        return memoryCache;
    }

    boolean updateCache(List<Object> cacheList, Object value) {
        if (!memoryCache.containsKey(cacheList)) {
            memoryCache.put(cacheList, value);
            return true;
        }
        return false;
    }

}
