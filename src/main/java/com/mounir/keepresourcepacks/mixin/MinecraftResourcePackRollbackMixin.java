package com.mounir.keepresourcepacks.mixin;

import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents vanilla's resource-pack rollback from clearing the selected pack list
 * after a runtime/manual reload failure. Startup recovery is left untouched.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftResourcePackRollbackMixin {
    @Shadow @Final private ResourceLoadStateTracker reloadStateTracker;

    @Inject(
        method = "rollbackResourcePacks",
        at = @At("HEAD"),
        cancellable = true
    )
    private void keepSelectedResourcePacksOnRuntimeFailure(
        Throwable throwable,
        GameLoadCookie loadCookie,
        CallbackInfo ci
    ) {
        Minecraft minecraft = (Minecraft) (Object) this;

        // Never interfere with initial startup recovery.
        if (!minecraft.isGameLoadFinished()) {
            return;
        }

        /*
         * The failed reload never became the active resource state, so the
         * previously active resources remain in use. Finish the tracker state
         * and remove the loading overlay, but do NOT call vanilla rollback,
         * which would clear resourcePackRepository/options.resourcePacks.
         */
        reloadStateTracker.finishReload();
        minecraft.gui.setOverlay(null);
        minecraft.options.save();

        ci.cancel();
    }
}
