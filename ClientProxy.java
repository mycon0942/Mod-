package com.silentwatch;

import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    @Override
    public void registerClient() {
        RenderingRegistry.registerEntityRenderingHandler(EntityWatcher.class, new RenderWatcher());
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }
}
