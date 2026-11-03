package com.silentwatch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/** Renderiza o Observador com a skin do proprio jogador. */
public class RenderWatcher extends RenderLiving {
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/steve.png");

    public RenderWatcher() {
        super(new ModelBiped(), 0.0F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        Object p = Minecraft.getMinecraft().thePlayer;
        if (p instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) p).getLocationSkin();
        }
        return STEVE;
    }
}
