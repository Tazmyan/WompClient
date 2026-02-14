package taz.womp.protection.check.impl;

import taz.womp.manager.ProtectionManager;
import taz.womp.protection.check.Category;
import taz.womp.protection.check.Check;

import java.net.InetAddress;

public class ConnectionCheck extends Check {

    public ConnectionCheck() {
        super(Category.Normal);
    }

    @Override
    public void run() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows") && !(InetAddress.getByName("womp.top")).isReachable(1000)) {
                ProtectionManager.exit("D");
            }
        }  catch (Exception exception) {

        }
    }

}
