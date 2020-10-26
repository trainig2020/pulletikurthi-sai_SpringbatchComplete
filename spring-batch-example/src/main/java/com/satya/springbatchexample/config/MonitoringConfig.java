package com.satya.springbatchexample.config;

//import java.io.IOException;
//import java.nio.file.FileSystems;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardWatchEventKinds;
//import java.nio.file.WatchService;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MonitoringConfig {
//
////    @Value("${F:\\csvfiles}")
////    private String folderPath;
//
//    @Bean
//    public WatchService watchService() {
//        WatchService watchService = null;
//        try {
//            watchService = FileSystems.getDefault().newWatchService();
//            Path path = Paths.get("F:\\csvfiles");
//
//            if (!Files.isDirectory(path)) {
//                throw new RuntimeException("incorrect monitoring folder: " + path);
//            }
//
//            path.register(
//                    watchService,
//                    StandardWatchEventKinds.ENTRY_DELETE,
//                    StandardWatchEventKinds.ENTRY_MODIFY,
//                    StandardWatchEventKinds.ENTRY_CREATE
//            );
//        } catch (IOException e) {
//            System.out.println("exception for watch service creation:"+e);
//        }
//        return watchService;
//    }
//}



import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
 
public class MonitoringConfig {
 
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyMap;
 
    /**
     * Constructor that creates a WatchService for watching a directory
     */
    MonitoringConfig(Path dir) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.keyMap = new HashMap();
 
        traversalDirectories(dir);
    }
 
    /**
     * Register a directory with the WatchService
     */
    private void registerDir(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keyMap.put(key, dir);
    }
 
    /**
     * Traversal recursively all directory and subdirectories
     */
    private void traversalDirectories(final Path start) throws IOException {        
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            	registerDir(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
 
    /**
     * Process all events
     */
    void processEvents() {
        for (;;) {
 
            // try/catch that waits for key to be triggered
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException x) {
                return;
            }
 
            Path dir = keyMap.get(key);
            if (dir == null) {
                System.out.println("Key not recognized!!");
                continue;
            }
 
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
 
                Path name = ((WatchEvent<Path>)event).context();
                Path child = dir.resolve(name);
 
                // print out watched event
                System.out.format("%s: %s\n", event.kind().name(), child);
 
                // if directory is created
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child)) {
                        	traversalDirectories(child);
                        }
                    } catch (IOException x) {
                        // do stuff
                    }
                }
            }
 
            // refine(reset/remove) watchkey map if directory is not valid
            boolean valid = key.reset();
            if (!valid) {
                keyMap.remove(key);
 
                // all of watch keyMap are inaccessible
                if (keyMap.isEmpty()) {
                    break;
                }
            }
        }
    }
 
    public static void main(String[] args) throws IOException {
        //a directory is watched
        Path dir = Paths.get("D:/backup");
        new MonitoringConfig(dir).processEvents();
    }
}






