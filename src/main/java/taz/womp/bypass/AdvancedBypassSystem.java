package taz.womp.bypass;

import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.AdvancedMemoryCleaner;

import java.util.ArrayList;
import java.util.List;
// Shit aint advanced dawg its ass
public class AdvancedBypassSystem {
    
    private final List<BypassMethod> bypassMethods = new ArrayList<>();
    private long lastExecution = 0;
    
    public AdvancedBypassSystem() {
        initializeBypassMethods();
        Womp.INSTANCE.getEventBus().register(this);
    }
       private void initializeBypassMethods() {
        bypassMethods.add(new LogBypass());
        bypassMethods.add(new MemoryBypass());
        bypassMethods.add(new StringBypass());
        bypassMethods.add(new ProcessBypass());
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExecution >= 5000) { 
            executeBypasses();
            lastExecution = currentTime;
        }
    }

    private void executeBypasses() {
        for (BypassMethod bypass : bypassMethods) {
            try {
                bypass.execute();
            } catch (Exception e) {
                
            }
        }
    }
 
    public void activate() {
        executeBypasses();
    }

    public void deactivate() {
        
    }

    private abstract static class BypassMethod {
        protected abstract void execute();
        protected abstract EncryptedString getName();
    }

    private static class LogBypass extends BypassMethod {
        @Override
        protected void execute() {
            AdvancedMemoryCleaner.clearLogFiles();
        }
        
        @Override
        protected EncryptedString getName() {
            return EncryptedString.of("Log Bypass");
        }
    }

    private static class MemoryBypass extends BypassMethod {
        @Override
        protected void execute() {
            
            System.gc();
            try {
                System.runFinalization();
            } catch (Exception e) {
                
            }
        }
        
        @Override
        protected EncryptedString getName() {
            return EncryptedString.of("Memory Bypass");
        }
    }

    private static class StringBypass extends BypassMethod {
        @Override
        protected void execute() {
            
            try {
                
                System.gc();
            } catch (Exception e) {
                
            }
        }
        
        @Override
        protected EncryptedString getName() {
            return EncryptedString.of("String Bypass");
        }
    }

    private static class ProcessBypass extends BypassMethod {
        @Override
        protected void execute() {
            
            try {
                
                String[] propertiesToCheck = {
                    "java.class.path",
                    "sun.boot.class.path"
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
        
        @Override
        protected EncryptedString getName() {
            return EncryptedString.of("Process Bypass");
        }
    }
}
