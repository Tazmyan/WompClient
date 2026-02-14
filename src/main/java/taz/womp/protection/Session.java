package taz.womp.protection;

public class Session {
    public int uid;
    public String username;
    public String email;
    public String id;
    public String hwid;
    public String jwtToken;

    public Session(int n, String string, String string2, String string3, String string4, String jwtToken) {
        this.uid = n;
        this.username = string;
        this.email = string2;
        this.id = string3;
        this.hwid = string4;
        this.jwtToken = jwtToken;
    }

    public int getUid() {
        return this.uid;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getID() {
        return this.id;
    }

    public String getHWID() {
        return this.hwid;
    }

    public String getJwtToken() {
        return this.jwtToken;
    }

    @Override
    public String toString() {
        return "Session{" +
                "uid=" + uid +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", id='" + id + '\'' +
                ", hwid='" + hwid + '\'' +
                '}';
    }
}
