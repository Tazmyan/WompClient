package taz.womp.utils;

import java.awt.*;

public final class ColorUtil {
    public static Color a(final int n, final int a) {
        final Color hsbColor = Color.getHSBColor((System.currentTimeMillis() * 3L + n * 175) % 7200L / 7200.0f, 0.6f, 1.0f);
        return new Color(hsbColor.getRed(), hsbColor.getGreen(), hsbColor.getBlue(), a);
    }

    public static Color alphaStep_NumberOne(final Color color, final int n, final int n2) {
        final float[] hsbvals = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbvals);
        hsbvals[2] = 0.25f + 0.75f * Math.abs((System.currentTimeMillis() % 2000L / 1000.0f + n / (float) n2 * 2.0f) % 2.0f - 1.0f) % 2.0f;
        final int hsBtoRGB = Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]);
        return new Color(hsBtoRGB >> 16 & 0xFF, hsBtoRGB >> 8 & 0xFF, hsBtoRGB & 0xFF, color.getAlpha());
    }

    public static Color a(final float n, final Color color, final Color color2) {
        return new Color((int) MathUtil.approachValue(n, color2.getRed(), color.getRed()), (int) MathUtil.approachValue(n, color2.getGreen(), color.getGreen()), (int) MathUtil.approachValue(n, color2.getBlue(), color.getBlue()));
    }

    public static Color a(final float n, final int n2, final Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) MathUtil.approachValue(n, color.getAlpha(), n2));
    }

    public static Color a(final Color color, final Color color2, final float n) {
        return new Color(a(Math.round(color.getRed() + n * (color2.getRed() - color.getRed())), 0, 255), a(Math.round(color.getGreen() + n * (color2.getGreen() - color.getGreen())), 0, 255), a(Math.round(color.getBlue() + n * (color2.getBlue() - color.getBlue())), 0, 255), a(Math.round(color.getAlpha() + n * (color2.getAlpha() - color.getAlpha())), 0, 255));
    }

    private static int a(final int b, final int a, final int a2) {
        return Math.max(a, Math.min(a2, b));
    }


    public static float rainbowHue(final long currentTimeMillis, final float speed) {
        final float clampedSpeed = Math.max(0.01f, speed);

        final float periodMs = 10000.0f / clampedSpeed;
        return (currentTimeMillis % (long) periodMs) / periodMs;
    }


    public static Color rainbowColor(final float baseHue, final float hueOffset, final float saturation, final float brightness, final int alpha) {
        float hue = baseHue + hueOffset;
        hue = hue - (float) Math.floor(hue); // wrap to [0,1)
        final int rgb = Color.HSBtoRGB(hue, a((int) (saturation * 255f), 0, 255) / 255.0f, a((int) (brightness * 255f), 0, 255) / 255.0f);
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        return new Color(r, g, b, a(alpha, 0, 255));
    }
}
