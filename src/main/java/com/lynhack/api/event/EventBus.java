package com.lynhack.api.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    private final List<FrameListener> frameListeners = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends Event>, List<Consumer<Event>>> eventListeners = new ConcurrentHashMap<>();

    private EventBus() {}

    /**
     * Registers a listener for fast-path per-frame execution (zero reflection).
     */
    public void registerFrameListener(FrameListener listener) {
        if (listener != null && !frameListeners.contains(listener)) {
            frameListeners.add(listener);
        }
    }

    /**
     * Unregisters a fast-path per-frame listener.
     */
    public void unregisterFrameListener(FrameListener listener) {
        if (listener != null) {
            frameListeners.remove(listener);
        }
    }

    /**
     * Dispatches the high-frequency FrameUpdateEvent directly to all registered frame listeners.
     */
    public void postFrame(FrameUpdateEvent event) {
        for (int i = 0; i < frameListeners.size(); i++) {
            frameListeners.get(i).onFrame(event);
        }
    }

    /**
     * Subscribes a consumer for a specific event type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> consumer) {
        eventListeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                      .add((Consumer<Event>) consumer);
    }

    /**
     * Unsubscribes a consumer for an event type.
     */
    public <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> consumer) {
        List<Consumer<Event>> list = eventListeners.get(eventClass);
        if (list != null) {
            list.remove(consumer);
        }
    }

    /**
     * Dispatches a standard typed event.
     */
    public <T extends Event> T post(T event) {
        List<Consumer<Event>> list = eventListeners.get(event.getClass());
        if (list != null && !list.isEmpty()) {
            for (Consumer<Event> consumer : list) {
                consumer.accept(event);
                if (event.isCancelled()) {
                    break;
                }
            }
        }
        return event;
    }
}
