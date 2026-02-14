package taz.womp.utils;

import com.sun.jna.Memory;
import taz.womp.Womp;
import taz.womp.module.Module;
import taz.womp.module.setting.Setting;
import taz.womp.module.setting.StringSetting;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AdvancedMemoryCleaner {
    
    private static final Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
    private static final AtomicLong modificationCounter = new AtomicLong();

    public static void performAdvancedCleanup(boolean enableUSNJournal, boolean enableFileCleanup) {
        try {
            
            clearAllModuleStrings();
            
            
            clearStaticStringReferences();
            
            
            performAdvancedMemoryCleanup();
            
            
            if (enableUSNJournal) {
                floodUSNJournal();
            }
            
            
            if (enableFileCleanup) {
                performFileSystemCleanup();
            }
            
            
            performFinalCleanup();
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Performs comprehensive memory cleanup and string obfuscation
     * @param enableUSNJournal Whether to enable USN Journal flooding
     */
    public static void performAdvancedCleanup(boolean enableUSNJournal) {
        performAdvancedCleanup(enableUSNJournal, false);
    }
    
    /**
     * Performs comprehensive memory cleanup and string obfuscation
     * Default version - USN Journal flooding disabled
     */
    public static void performAdvancedCleanup() {
        performAdvancedCleanup(false);
    }
    
    /**
     * Clears all module names, descriptions, and settings
     */
    private static void clearAllModuleStrings() {
        try {
            for (Module module : Womp.INSTANCE.getModuleManager().c()) {
                
                clearModuleStrings(module);
                
                
                if (module.getSettings() != null) {
                    for (Setting setting : module.getSettings()) {
                        clearSettingStrings(setting);
                    }
                    module.getSettings().clear();
                }
            }
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears module strings using reflection
     */
    private static void clearModuleStrings(Module module) {
        try {
            
            Field[] fields = module.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    field.set(module, null);
                }
            }
            
            
            Class<?> parentClass = module.getClass().getSuperclass();
            while (parentClass != null && parentClass != Object.class) {
                Field[] parentFields = parentClass.getDeclaredFields();
                for (Field field : parentFields) {
                    if (field.getType() == String.class) {
                        field.setAccessible(true);
                        field.set(module, null);
                    }
                }
                parentClass = parentClass.getSuperclass();
            }
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears setting strings
     */
    private static void clearSettingStrings(Setting setting) {
        try {
            if (setting instanceof StringSetting) {
                ((StringSetting) setting).setValue(null);
            }
            
            
            setting.setDescription(null);
            
            
            Field[] fields = setting.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    field.set(setting, null);
                }
            }
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears static string references throughout the application
     */
    private static void clearStaticStringReferences() {
        try {
            
            clearWompInstanceStrings();
            
            
            clearCachedStrings();
            
            
            clearSystemPropertyTraces();
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears strings from Womp instance
     */
    private static void clearWompInstanceStrings() {
        try {
            
            Field[] fields = Womp.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    field.set(Womp.INSTANCE, null);
                }
            }
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears cached strings
     */
    private static void clearCachedStrings() {
        try {
            
            
            
            
            System.gc();
            System.runFinalization();
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears system property traces
     */
    private static void clearSystemPropertyTraces() {
        try {
            
            String[] propertiesToCheck = {
                "java.class.path",
                "sun.boot.class.path",
                "java.ext.dirs"
            };
            
            for (String prop : propertiesToCheck) {
                String value = System.getProperty(prop);
                if (value != null && value.toLowerCase().contains("womp")) {
                    
                    System.clearProperty(prop);
                }
            }
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Performs advanced memory cleanup using multiple techniques
     */
    private static void performAdvancedMemoryCleanup() {
        Runtime runtime = Runtime.getRuntime();
        
        
        for (int i = 0; i <= 15; ++i) {
            runtime.gc();
            runtime.runFinalization();
            
            try {
                Thread.sleep(50 * i);
                
                
                Memory.purge();
                Memory.disposeAll();
                
                
                System.gc();
                System.runFinalization();
                
            } catch (InterruptedException e) {
                
            } catch (Exception e) {
                
            }
        }
    }
    
    /**
     * Floods the USN Journal to hide file system traces
     */
    private static void floodUSNJournal() {
        try {
            Path[] pathArray = new Path[50]; 
            ExecutorService executorService = Executors.newWorkStealingPool(50);
            CountDownLatch countDownLatch = new CountDownLatch(50);
            
            
            for (int i = 0; i < 50; ++i) {
                final int n = i;
                executorService.submit(() -> {
                    try {
                        pathArray[n] = Files.createTempFile(tempDirectory, "meta", ".tmp");
                        countDownLatch.countDown();
                    } catch (Throwable t) {
                        
                    }
                });
            }
            
            countDownLatch.await();
            
            
            for (int i = 0; i < 50; ++i) {
                Path path = pathArray[i];
                executorService.submit(() -> {
                    while (modificationCounter.get() < 1000000L) { 
                        try {
                            Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
                            boolean bl = !((Boolean) Files.getAttribute(path, "dos:archive"));
                            Files.setAttribute(path, "dos:archive", bl);
                        } catch (IOException e) {
                            
                        }
                        modificationCounter.addAndGet(2L);
                    }
                });
            }
            
            executorService.shutdown();
            executorService.awaitTermination(2L, TimeUnit.HOURS); 
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Performs aggressive file system cleanup to hide traces
     */
    private static void performFileSystemCleanup() {
        try {
            
            Path[] tempFiles = new Path[100];
            ExecutorService executor = Executors.newWorkStealingPool(50);
            
            
            for (int i = 0; i < 100; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        tempFiles[index] = Files.createTempFile(tempDirectory, "cleanup", ".tmp");
                        
                        for (int j = 0; j < 10; j++) {
                            Files.setLastModifiedTime(tempFiles[index], FileTime.fromMillis(System.currentTimeMillis()));
                            Files.setAttribute(tempFiles[index], "dos:archive", j % 2 == 0);
                            Files.setAttribute(tempFiles[index], "dos:hidden", j % 3 == 0);
                        }
                    } catch (Exception e) {
                        
                    }
                });
            }
            
            
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            
            
            for (Path file : tempFiles) {
                if (file != null) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (Exception e) {
                        
                    }
                }
            }
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Performs final cleanup operations
     */
    private static void performFinalCleanup() {
        try {
            
            for (int i = 0; i < 10000; i++) {
                new Object();
            }
            
            
            System.gc();
            System.runFinalization();
            System.gc();
            
            
            Memory.purge();
            Memory.disposeAll();
            
            
            System.gc();
            System.runFinalization();
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Clears log files with advanced techniques
     */
    public static void clearLogFiles() {
        try {
            File logFile = new File(System.getProperty("user.dir") + File.separator + "logs" + File.separator + "latest.log");
            if (!logFile.exists()) {
                return;
            }
            
            
            String jarFileName = "";
            String jarFileNameEncoded = "";
            try {
                File currentJar = getCurrentJarPath();
                if (currentJar.exists()) {
                    jarFileName = currentJar.getName();
                    try {
                        jarFileName = java.net.URLDecoder.decode(jarFileName, "UTF-8");
                    } catch (Exception ignored) {}
                    if (jarFileName.contains("\\") || jarFileName.contains("/")) {
                        String[] pathParts = jarFileName.split("[\\\\/]");
                        jarFileName = pathParts[pathParts.length - 1];
                    }
                    try {
                        jarFileNameEncoded = java.net.URLEncoder.encode(jarFileName, "UTF-8");
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            
            
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            StringBuilder cleanedContent = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                
                
                if (!lowerLine.contains("womp") && 
                    !lowerLine.contains("taz") &&
                    !lowerLine.contains("[main/info] (fabricloader) loading ") && 
                    (jarFileName.isEmpty() || (!line.contains(jarFileName) && !line.contains(jarFileNameEncoded)))) {
                    cleanedContent.append(line).append("\n");
                } else if (lowerLine.contains("womp") || lowerLine.contains("taz")) {
                    
                    cleanedContent.append(line.replaceAll("(?i)womp|taz", "")).append("\n");
                }
                
                
                if (lowerLine.contains("[main/info] (fabricloader) loading ")) {
                    try {
                        String modCount = line.split("Loading ")[1].split(" mods:")[0];
                        cleanedContent.append(line.split("Loading ")[0])
                                     .append(Integer.parseInt(modCount) - 1)
                                     .append(" mods:")
                                     .append("\n");
                    } catch (Exception e) {
                        cleanedContent.append(line).append("\n");
                    }
                }
            }
            reader.close();
            
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(cleanedContent.toString());
            writer.close();
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * Gets the current JAR path
     */
    private static File getCurrentJarPath() {
        try {
            return new File(AdvancedMemoryCleaner.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (Exception e) {
            return null;
        }
    }
}
