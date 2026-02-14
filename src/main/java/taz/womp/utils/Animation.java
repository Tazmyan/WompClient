package taz.womp.utils;

import taz.womp.module.modules.client.WompModule;

public final class Animation {
    private double value;
    private final double end;

    public Animation(double end) {
        this.value = end;
        this.end = end;
    }

    public void animate(double speed, double target) {
        if (WompModule.animationMode.isMode(WompModule.AnimationMode.NORMAL)) {
            this.value = MathUtil.approachValue((float) speed, this.value, target);
        } else if (WompModule.animationMode.isMode(WompModule.AnimationMode.POSITIVE)) {
            this.value = MathUtil.smoothStep(speed, this.value, target);
        } else if (WompModule.animationMode.isMode(WompModule.AnimationMode.OFF)) {
            this.value = target;
        }
    }

    public double getAnimation() {
        return this.value;
    }

    public void setAnimation(final double factor) {
        this.value = MathUtil.smoothStep(factor, this.value, this.end);
    }
}
