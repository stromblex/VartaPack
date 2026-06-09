package com.stromblex.vartapack.forge;

import com.stromblex.vartapack.VartaPack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(VartaPack.MOD_ID)
public final class VartaPackForge {
    @SuppressWarnings("removal")
    public VartaPackForge() {
        VartaPack.init(new ForgePlatform());
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VartaPackForgeClient.init(modBus));
    }
}
