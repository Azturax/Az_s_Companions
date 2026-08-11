package com.azscompanions.compat.dynamiclights;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Optional reflection hooks for older Dynamic Lights APIs (pre-LDL-v4 / RyoamicLights forks)
 * that register per-{@code EntityType} handlers via {@code DynamicLightHandlers}.
 * No compile-time dependency — safe no-op when classes are absent.
 */
public final class DynamicLightsLegacyHooks {
    private DynamicLightsLegacyHooks() {
    }

    /**
     * Attempts to register a living-entity (held-item) light handler for the companion entity type.
     *
     * @param entityTypeSupplier supplies the registered {@code EntityType<?>} (loader-specific)
     * @return true if a registration call succeeded
     */
    public static boolean tryRegisterLivingEntityHandler(Supplier<Object> entityTypeSupplier) {
        return tryRegisterLivingEntityHandler(entityTypeSupplier, entity -> 0);
    }

    /**
     * Same as {@link #tryRegisterLivingEntityHandler(Supplier)} with an extra luminance source
     * (merged with held-item scanning when {@code makeLivingEntityHandler} exists).
     *
     * @param extraLuminance receives the entity instance; return 0–15
     */
    public static boolean tryRegisterLivingEntityHandler(
            Supplier<Object> entityTypeSupplier,
            ToIntFunction<Object> extraLuminance
    ) {
        if (!DynamicLightsCompat.shouldApplyHooks()) {
            return false;
        }
        Object entityType;
        try {
            entityType = entityTypeSupplier.get();
        } catch (Throwable t) {
            return false;
        }
        if (entityType == null) {
            return false;
        }
        ToIntFunction<Object> luminance = extraLuminance == null ? entity -> 0 : extraLuminance;

        String[] handlerOwnerCandidates = {
                "dev.lambdaurora.lambdynlights.api.DynamicLightHandlers",
                "org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers",
                "me.lambdaurora.lambdynlights.api.DynamicLightHandlers"
        };
        String[] handlerTypeCandidates = {
                "dev.lambdaurora.lambdynlights.api.DynamicLightHandler",
                "org.thinkingstudio.ryoamiclights.api.DynamicLightHandler",
                "me.lambdaurora.lambdynlights.api.DynamicLightHandler"
        };

        for (int i = 0; i < handlerOwnerCandidates.length; i++) {
            if (tryRegister(handlerOwnerCandidates[i], handlerTypeCandidates[i], entityType, luminance)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryRegister(
            String handlersClassName,
            String handlerClassName,
            Object entityType,
            ToIntFunction<Object> extraLuminance
    ) {
        try {
            Class<?> handlers = Class.forName(handlersClassName);
            Class<?> handler = Class.forName(handlerClassName);

            Object livingHandler;
            Object extra = proxyLuminanceHandler(handler, extraLuminance);
            if (extra == null) {
                return false;
            }
            try {
                Method makeLiving = handler.getMethod("makeLivingEntityHandler", handler);
                // makeLivingEntityHandler merges held-item scanning with our extra luminance.
                livingHandler = makeLiving.invoke(null, extra);
            } catch (NoSuchMethodException noMake) {
                livingHandler = extra;
            }
            if (livingHandler == null) {
                return false;
            }

            Method register = handlers.getMethod("registerDynamicLightHandler",
                    Class.forName("net.minecraft.world.entity.EntityType"), handler);
            register.invoke(null, entityType, livingHandler);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Builds a DynamicLightHandler whose {@code getLuminance} delegates to {@code luminanceFn}.
     */
    private static Object proxyLuminanceHandler(Class<?> handlerInterface, ToIntFunction<Object> luminanceFn) {
        if (!handlerInterface.isInterface()) {
            return null;
        }
        return java.lang.reflect.Proxy.newProxyInstance(
                handlerInterface.getClassLoader(),
                new Class<?>[]{handlerInterface},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getLuminance".equals(name)) {
                        Object entity = (args != null && args.length > 0) ? args[0] : null;
                        return entity == null ? 0 : Math.max(0, Math.min(15, luminanceFn.applyAsInt(entity)));
                    }
                    if ("isWaterSensitive".equals(name)) {
                        return false;
                    }
                    if ("equals".equals(name)) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("toString".equals(name)) {
                        return "AzsCompanionsDynamicLightHandler";
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (rt == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}
