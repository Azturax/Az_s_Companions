package com.azscompanions.compat.cci;

import com.azscompanions.cci.CciMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Loader-local Component helpers for {@link CciMessages} keys. */
final class CciMsg {
    private CciMsg() {
    }

    static MutableComponent t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    static MutableComponent title(String key) {
        return Component.translatable(key);
    }

    static MutableComponent named(String companionName) {
        return Component.literal(companionName == null ? "" : companionName);
    }

    static MutableComponent actionBar(Component title, Component body) {
        return Component.translatable(CciMessages.ACTIONBAR, title, body);
    }

    static String plain(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}
