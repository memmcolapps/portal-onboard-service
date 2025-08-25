//package org.memmcol.portalonboardservice.util;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.hazelcast.map.MapStore;
////import com.hazelcast.core.MapStore;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.*;
//
//public class NodeCacheMapStore implements MapStore<String, String> {
//
//    private static final String FILE_PATH = "portalNodeCache.json";
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    private Map<String, String> loadFromFile() {
//        File file = new File(FILE_PATH);
//        if (!file.exists()) {
//            return new HashMap<>();
//        }
//        try {
//            return objectMapper.readValue(file, new TypeReference<Map<String, String>>() {});
//        } catch (IOException e) {
//            e.printStackTrace();
//            return new HashMap<>();
//        }
//    }
//
//    private void saveToFile(Map<String, String> data) {
//        try {
//            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), data);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void store(String key, String value) {
//        Map<String, String> data = loadFromFile();
//        data.put(key, value);
//        saveToFile(data);
//    }
//
//    @Override
//    public void storeAll(Map<String, String> map) {
//        Map<String, String> data = loadFromFile();
//        data.putAll(map);
//        saveToFile(data);
//    }
//
//    @Override
//    public void delete(String key) {
//        Map<String, String> data = loadFromFile();
//        data.remove(key);
//        saveToFile(data);
//    }
//
//    @Override
//    public void deleteAll(Collection<String> keys) {
//        Map<String, String> data = loadFromFile();
//        keys.forEach(data::remove);
//        saveToFile(data);
//    }
//
//    @Override
//    public String load(String key) {
//        return loadFromFile().get(key);
//    }
//
//    @Override
//    public Map<String, String> loadAll(Collection<String> keys) {
//        Map<String, String> data = loadFromFile();
//        Map<String, String> result = new HashMap<>();
//        for (String key : keys) {
//            if (data.containsKey(key)) {
//                result.put(key, data.get(key));
//            }
//        }
//        return result;
//    }
//
//    @Override
//    public Iterable<String> loadAllKeys() {
//        return loadFromFile().keySet();
//    }
//}
//
