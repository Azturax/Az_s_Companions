package com.azscompanions.compat.map;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Loader-agnostic companion detection for map plugins (reflection — no Minecraft types).
 */
public final class CompanionMapEntity {
    private CompanionMapEntity() {
    }

    public static boolean isCompanion(Object entity) {
        if (entity == null) {
            return false;
        }
        String name = entity.getClass().getName();
        return name.endsWith("CompanionEntity") || name.endsWith("FabricCompanionEntity");
    }

    public static boolean isChildCompanion(Object entity) {
        Boolean child = invokeBoolean(entity, "isChildCompanion");
        if (child != null) {
            return child;
        }
        Boolean fight = invokeBoolean(entity, "isFightSpawn");
        return fight != null && fight;
    }

    public static String displayName(Object entity) {
        if (entity == null) {
            return "Companion";
        }
        try {
            Method m = entity.getClass().getMethod("getChatDisplayName");
            Object v = m.invoke(entity);
            if (v != null) {
                String s = v.toString().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // fall through
        }
        try {
            Method m = entity.getClass().getMethod("getName");
            Object v = m.invoke(entity);
            if (v != null) {
                Method getString = v.getClass().getMethod("getString");
                Object s = getString.invoke(v);
                if (s != null) {
                    String text = s.toString().trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // fall through
        }
        return "Companion";
    }

    public static String ownerNameHint(Object entity) {
        try {
            Method ownerUuid = entity.getClass().getMethod("getOwnerUuid");
            Object uuid = ownerUuid.invoke(entity);
            if (!(uuid instanceof UUID)) {
                return null;
            }
            Method getOwner = entity.getClass().getMethod("getOwner");
            Object owner = getOwner.invoke(entity);
            if (owner == null) {
                return null;
            }
            Method profile = owner.getClass().getMethod("getGameProfile");
            Object gp = profile.invoke(owner);
            if (gp == null) {
                return null;
            }
            Method getName = gp.getClass().getMethod("getName");
            Object name = getName.invoke(gp);
            return name == null ? null : name.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Boolean invokeBoolean(Object entity, String method) {
        try {
            Method m = entity.getClass().getMethod(method);
            Object v = m.invoke(entity);
            return v instanceof Boolean b ? b : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
