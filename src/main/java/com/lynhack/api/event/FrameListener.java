package com.lynhack.api.event;

@FunctionalInterface
public interface FrameListener {
    void onFrame(FrameUpdateEvent event);
}
