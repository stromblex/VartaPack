package com.stromblex.vartapack.forge;

import com.stromblex.vartapack.VartaPack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(VartaPack.MOD_ID)
public final class VartaPackForge {
    public VartaPackForge() {
        VartaPack.init(new ForgePlatform());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VartaPackForgeClient::init);
    }
}
