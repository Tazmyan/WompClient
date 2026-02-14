package taz.womp.protection;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Optional;

public class HWIDGrabber {

    public static String getHWID() {
        String os = Optional.ofNullable(System.getenv("os")).orElse("unknown");
        String OS = Optional.ofNullable(System.getenv("OS")).orElse("unknown");
        String arch = Optional.ofNullable(System.getenv("PROCESSOR_ARCHITECTURE")).orElse("unknown");
        String user = Optional.ofNullable(System.getenv("USERNAME")).orElse("unknown");
        String systemRoot = Optional.ofNullable(System.getenv("SystemRoot")).orElse("unknown");
        String homeDrive = Optional.ofNullable(System.getenv("HOMEDRIVE")).orElse("unknown");
        String procLevel = Optional.ofNullable(System.getenv("PROCESSOR_LEVEL")).orElse("unknown");
        String procRev = Optional.ofNullable(System.getenv("PROCESSOR_REVISION")).orElse("unknown");
        String procId = Optional.ofNullable(System.getenv("PROCESSOR_IDENTIFIER")).orElse("unknown");
        String arch2 = Optional.ofNullable(System.getenv("PROCESSOR_ARCHITECTURE")).orElse("unknown");
        String archW6432 = Optional.ofNullable(System.getenv("PROCESSOR_ARCHITEW6432")).orElse("unknown");
        String numProc = Optional.ofNullable(System.getenv("NUMBER_OF_PROCESSORS")).orElse("unknown");
        String hwidString = os + OS + arch + user + systemRoot + homeDrive + procLevel + procRev + procId + arch2 + archW6432 + numProc;
        return DigestUtils.sha256Hex(hwidString);
    }
}
