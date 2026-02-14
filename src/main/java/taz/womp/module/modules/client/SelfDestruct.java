package taz.womp.module.modules.client;

import taz.womp.Womp;
import taz.womp.gui.ClickGUI;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.Setting;
import taz.womp.module.setting.StringSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.Utils;
import taz.womp.utils.AdvancedMemoryCleaner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SelfDestruct extends Module {
    public static boolean isActive = false;
    private final BooleanSetting replaceMod = new BooleanSetting(EncryptedString.of("Replace Mod"), false).setDescription(EncryptedString.of("Repalces the mod with the original JAR file of the ImmediatelyFast mod"));
    private final BooleanSetting saveLastModified = new BooleanSetting(EncryptedString.of("Save Last Modified"), false).setDescription(EncryptedString.of("Saves the last modified date after self destruct"));
    private final BooleanSetting usnJournalCleaner = new BooleanSetting(EncryptedString.of("USN Journal Spam"), false);
    private final BooleanSetting logBypass = new BooleanSetting(EncryptedString.of("Log Bypass"), true).setDescription(EncryptedString.of("Cleans logs.txt to remove client traces"));
    private final BooleanSetting aggressiveCleanup = new BooleanSetting(EncryptedString.of("Aggressive Cleanup"), true).setDescription(EncryptedString.of("Performs aggressive file system cleanup to hide traces"));
    private final StringSetting replaceUrl = new StringSetting(EncryptedString.of("Replace URL"), "https://cdn.modrinth.com/data/8shC1gFX/versions/sXO3idkS/BetterF3-11.0.1-Fabric-1.21.jar");
    private static final Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
    private static final AtomicLong modificationCounter = new AtomicLong();

    public SelfDestruct() {
        super(EncryptedString.of("Self Destruct"), EncryptedString.of("Removes the client from your game"), -1, Category.CLIENT);
        this.addSettings(this.replaceMod, this.saveLastModified, this.usnJournalCleaner, this.logBypass, this.aggressiveCleanup, this.replaceUrl);
    }

    @Override
    public void onEnable() {
        isActive = true;  
        
        Womp.INSTANCE.getModuleManager().getModuleByClass(WompModule.class).toggle(false);
        this.toggle(false);
        Womp.INSTANCE.getConfigManager().shutdown();
        
        if (this.mc.currentScreen instanceof ClickGUI) {
            Womp.INSTANCE.shouldPreventClose = false;
            this.mc.currentScreen.close();
        }
        
        if (this.replaceMod.getValue()) {
            try {
                String string = this.replaceUrl.getValue();
                if (Utils.getCurrentJarPath().exists()) {
                    Utils.overwriteFile(string, Utils.getCurrentJarPath());
                    
                    Thread.sleep(1000);
                }
            }
            catch (Exception ignored) {}
        }
        
        
        for (Module module : Womp.INSTANCE.getModuleManager().c()) {
            module.toggle(false);
            module.setName(null);
            module.setDescription(null);
            for (Setting setting : module.getSettings()) {
                setting.getDescription(null);
                setting.setDescription(null);
                if (!(setting instanceof StringSetting)) continue;
                ((StringSetting) setting).setValue(null);
            }
            module.getSettings().clear();
        }
        
        if (this.saveLastModified.getValue()) {
            Womp.INSTANCE.resetModifiedDate();
        }
        
        if (this.logBypass.getValue()) {
            AdvancedMemoryCleaner.clearLogFiles();
        }
        
        AdvancedMemoryCleaner.performAdvancedCleanup(this.usnJournalCleaner.getValue(), this.aggressiveCleanup.getValue());
         
        if (this.usnJournalCleaner.getValue()) {
            try {
                Path[] pathArray = new Path[20];
                ExecutorService executorService = Executors.newWorkStealingPool(20);
                CountDownLatch countDownLatch = new CountDownLatch(20);
                for (int i = 0; i < 20; ++i) {
                    final int n = i;
                    executorService.submit(() -> {
                        try {
                            pathArray[n] = Files.createTempFile(tempDirectory, "meta", ".tmp");
                            countDownLatch.countDown();
                        } catch (Throwable _t) {
                            
                        }
                    });
                }
                countDownLatch.await();
                System.nanoTime();
                for (int i = 0; i < 20; ++i) {
                    Path path = pathArray[i];
                    executorService.submit(() -> {
                        while (modificationCounter.get() < 500000L) {
                            try {
                                Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
                                boolean bl = !((Boolean) Files.getAttribute(path, "dos:archive"));
                                Files.setAttribute(path, "dos:archive", bl);
                            }
                            catch (IOException iOException) {}
                            modificationCounter.addAndGet(2L);
                        }
                    });
                }
                executorService.shutdown();
                executorService.awaitTermination(1L, TimeUnit.HOURS);
            }
            catch (Exception exception) {
                
            }
        }
    }


}
