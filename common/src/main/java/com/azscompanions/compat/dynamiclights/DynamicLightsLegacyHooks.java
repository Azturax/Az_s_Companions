package com.azscompanions.compat.dynamiclights;

import java.lang.reflect.Method;
import java.util.function.Supplier;

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
            if (tryRegister(handlerOwnerCandidates[i], handlerTypeCandidates[i], entityType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryRegister(String handlersClassName, String handlerClassName, Object entityType) {
        try {
            Class<?> handlers = Class.forName(handlersClassName);
            Class<?> handler = Class.forName(handlerClassName);

            Object livingHandler;
            try {
                Method makeLiving = handler.getMethod("makeLivingEntityHandler", handler);
                // Zero-luminance extra handler — makeLivingEntityHandler merges held-item scanning.
                Object empty = proxyEmptyHandler(handler);
                if (empty == null) {
                    return false;
                }
                livingHandler = makeLiving.invoke(null, empty);
            } catch (NoSuchMethodException noMake) {
                // Some forks only expose registerDynamicLightHandler(EntityType, DynamicLightHandler)
                livingHandler = proxyEmptyHandler(handler);
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
     * Builds a no-op DynamicLightHandler (getLuminance → 0) via a simple JDK proxy when the
     * interface is present; otherwise returns null and skips registration.
     */
    private static Object proxyEmptyHandler(Class<?> handlerInterface) {
        if (!handlerInterface.isInterface()) {
            return null;
        }
        return java.lang.reflect.Proxy.newProxyInstance(
                handlerInterface.getClassLoader(),
                new Class<?>[]{handlerInterface},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getLuminance".equals(name)) {
                        return 0;
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
