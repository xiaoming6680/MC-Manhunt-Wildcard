package com.xiaoming.hunterwildcard.client.ui;

import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.*;
import java.util.HashMap;
import java.util.Map;

/** Connection-scoped editor state. Broadcast syncs never acknowledge a save. */
public final class ConfigDraft {
    public static ConfigSnapshot base, value, latest;
    public static final Map<String, String> raw = new HashMap<>();
    public static long pendingId;
    private static long sequence, submittedAt;
    public static String message = "";
    public static boolean failed;
    private ConfigDraft() {}
    public static void sync(ConfigSnapshot config) {
        latest = config;
        if (value == null || (pendingId == 0 && value.equals(base) && raw.isEmpty() && !failed)) {
            base = value = config;
        }
    }
    public static long submit(ConfigSnapshot config) {
        value = config;
        failed = false;
        message = "";
        submittedAt = System.currentTimeMillis();
        pendingId = ++sequence;
        return pendingId;
    }
    public static boolean result(OperationResultPayload result) {
        if (pendingId == 0 || result.requestId() != pendingId) return false;
        pendingId = 0;
        failed = !result.success();
        message = result.message();
        if (result.success()) { base = value = latest; raw.clear(); }
        else if (result.message().endsWith("ui.save.disk_failed")) { base = latest; }
        return true;
    }
    public static void tick() {
        if(pendingId!=0 && System.currentTimeMillis()-submittedAt>15000){pendingId=0;failed=true;message="hunterwildcard.ui.save.timeout";}
    }
    public static boolean conflict() { return base != null && latest != null && !base.equals(latest); }
    public static void discard() { base = value = latest; raw.clear(); failed = false; message = ""; }
    public static void clear() { base = value = latest = null; raw.clear(); pendingId = 0; message = ""; failed = false; }
}
