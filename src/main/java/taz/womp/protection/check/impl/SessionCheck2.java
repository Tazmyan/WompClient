package taz.womp.protection.check.impl;

import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.protection.check.Category;
import taz.womp.protection.check.Check;

public class SessionCheck2 extends Check {

    public SessionCheck2() {
        super(Category.Session);
    }

    @Override
    public void run() {
        if (Womp.INSTANCE.getSession() == null) {
            ProtectionManager.exit("G");
        }
    }
}
