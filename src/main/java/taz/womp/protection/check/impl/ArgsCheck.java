/*
fool proof plan this was, gonna stop everyone from using java agents - thnkscj
*/
package taz.womp.protection.check.impl;

import taz.womp.manager.ProtectionManager;
import taz.womp.protection.check.Category;
import taz.womp.protection.check.Check;
import java.lang.management.ManagementFactory;

public class ArgsCheck extends Check {

    public static String[] args = {"-XBootclasspath", "-javaagent", "-Xdebug", "-agentlib", "-Xrunjdwp", "-Xnoagent", "-verbose", "-DproxySet", "-DproxyHost", "-DproxyPort", "Xrunjdwp:", "noverify"};

    public ArgsCheck() {
        super(Category.Normal);
    }

    @Override
    public void run() {
        try {
            for (String string : args) {
                if (ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains(string)) {
                    ProtectionManager.exit("B, " + string);
                }
                return;
            }
        } catch (Throwable throwable) {
            ProtectionManager.exit("B");
        }
    }
}
