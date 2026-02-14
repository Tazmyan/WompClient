package taz.womp.utils;

public class Rotation {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Rotation add(float yaw, float pitch) {
        return new Rotation(this.yaw + yaw, this.pitch + pitch);
    }

    public Rotation subtract(float yaw, float pitch) {
        return new Rotation(this.yaw - yaw, this.pitch - pitch);
    }

    public Rotation copy() {
        return new Rotation(this.yaw, this.pitch);
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
    }
} 