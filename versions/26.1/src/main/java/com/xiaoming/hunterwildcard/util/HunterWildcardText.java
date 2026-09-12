package com.xiaoming.hunterwildcard.util;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class HunterWildcardText {
    public static final String KEY_PREFIX = HunterWildcardMod.MOD_ID + ".";
    public static final String SPEC_SEPARATOR = "\u001F";

    private HunterWildcardText() {
    }

    public static String key(String path) {
        return KEY_PREFIX + path;
    }

    public static MutableComponent translatable(String path, Object... args) {
        return Component.translatable(key(path), args);
    }

    public static String spec(String path, Object... args) {
        String key = key(path);
        if (args == null || args.length == 0) {
            return key;
        }

        StringBuilder builder = new StringBuilder(key);
        for (Object arg : args) {
            builder.append(SPEC_SEPARATOR).append(arg == null ? "" : arg);
        }
        return builder.toString();
    }

    public static Component fromSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            return Component.empty();
        }

        if (!spec.startsWith(KEY_PREFIX)) {
            return Component.literal(spec);
        }

        String[] parts = spec.split(SPEC_SEPARATOR, -1);
        Object[] args = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
                .map(HunterWildcardText::argumentFromSpec)
                .toArray();
        return Component.translatable(parts[0], args);
    }

    public static MutableComponent prefixed(Component message) {
        return Component.empty()
                .append(translatable("msg.prefix"))
                .append(" ")
                .append(message);
    }

    public static MutableComponent prefixedSpec(String spec) {
        return prefixed(fromSpec(spec));
    }

    public static String wildcardId(String classSimpleName) {
        String baseName = classSimpleName.endsWith("Rule")
                ? classSimpleName.substring(0, classSimpleName.length() - "Rule".length())
                : classSimpleName;
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                id.append('_');
            }
            id.append(Character.toLowerCase(c));
        }
        return id.toString();
    }

    public static String wildcardNameKey(String wildcardId) {
        return key("wildcard." + wildcardId + ".name");
    }

    public static String wildcardDescriptionKey(String wildcardId) {
        return key("wildcard." + wildcardId + ".description");
    }

    public static MutableComponent wildcardName(String wildcardId) {
        return Component.translatable(wildcardNameKey(wildcardId));
    }

    private static Object argumentFromSpec(String value) {
        return value != null && value.startsWith(KEY_PREFIX) ? Component.translatable(value) : value;
    }
}
