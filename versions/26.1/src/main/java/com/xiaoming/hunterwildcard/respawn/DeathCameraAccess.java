package com.xiaoming.hunterwildcard.respawn;

import net.minecraft.world.entity.Entity;

/** Attaches an already-positioned death spectator without starting another vanilla teleport. */
public interface DeathCameraAccess {
    void hunterwildcard$setDeathCamera(Entity target);
}
