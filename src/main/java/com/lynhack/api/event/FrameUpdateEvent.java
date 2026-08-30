package com.lynhack.api.event;

public class FrameUpdateEvent extends Event {
    private final float tickDelta;
    private final long timeNanos;

    public FrameUpdateEvent(float tickDelta, long timeNanos) {
        this.tickDelta = tickDelta;
        this.timeNanos = timeNanos;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public long getTimeNanos() {
        return timeNanos;
    }
}
