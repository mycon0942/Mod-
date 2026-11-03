package com.silentwatch;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;

public class ClientEvents {

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity e) {
        if (!HorrorMod.thickFog) return;
        if (e.block.getMaterial() != Material.air) return; // nao mexe em agua/lava
        float d = HorrorMod.fogDensity;
        World w = Minecraft.getMinecraft().theWorld;
        if (w != null && w.isRaining()) d *= 1.4F;
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
        e.density = d;
        e.setCanceled(true);
    }

    @SubscribeEvent
    public void onFogColor(EntityViewRenderEvent.FogColors e) {
        if (!HorrorMod.thickFog) return;
        float g = (e.red + e.green + e.blue) / 3.0F; // dessatura
        e.red = g * 0.55F;
        e.green = g * 0.58F;
        e.blue = g * 0.60F;
    }
}
