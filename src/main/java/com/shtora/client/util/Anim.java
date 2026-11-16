package com.shtora.client.util;

public class Anim {
    public float v;

    public Anim(float v) { this.v = v; }

    /** Плавное приближение к цели (экспоненциальное сглаживание). */
    public float to(float target, float speed, float dt) {
        v += (target - v) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - v) < 0.001f) v = target;
        return v;
    }
}
