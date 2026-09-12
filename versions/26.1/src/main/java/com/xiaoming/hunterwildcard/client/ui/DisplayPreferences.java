package com.xiaoming.hunterwildcard.client.ui;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
/** Client-only preferences; never part of server rule snapshots. */
public final class DisplayPreferences {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("hunterwildcard-ui.json");
    public static DisplayPreferences get=load();
    public boolean compact=true, reducedMotion=false, killFlash=true, statusHud=false;
    public float scale=1.0F;
    public int opacity=85, inset=6, feedbackHoldSeconds=4;
    public boolean save() {
        try {Files.createDirectories(FILE.getParent());Files.writeString(FILE,GSON.toJson(this));return true;} catch(Exception e){return false;}
    }
    private static DisplayPreferences load() {
        try {if(Files.exists(FILE)){DisplayPreferences p=GSON.fromJson(Files.readString(FILE),DisplayPreferences.class);if(p!=null){p.scale=Float.isFinite(p.scale)?Math.clamp(p.scale,0.75F,1.25F):1;p.opacity=Math.clamp(p.opacity,50,100);p.inset=Math.clamp(p.inset,4,40);p.feedbackHoldSeconds=Math.clamp(p.feedbackHoldSeconds,3,8);return p;}}}catch(Exception ignored){}
        return new DisplayPreferences();
    }
}
