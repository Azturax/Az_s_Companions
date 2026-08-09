package com.koncompanions.voice;

import java.util.Optional;
import java.util.function.Function;

/**
 * Pluggable text-to-speech adapter. Default is a no-op; install a client adapter if available.
 */
public final class TtsVoiceAdapter {
    private static Function<String, Optional<Runnable>> ADAPTER = line -> Optional.empty();

    private TtsVoiceAdapter() {
    }

    public static void setAdapter(Function<String, Optional<Runnable>> adapter) {
        ADAPTER = adapter == null ? line -> Optional.empty() : adapter;
    }

    public static void speak(String line) {
        ADAPTER.apply(line).ifPresent(Runnable::run);
    }
}
