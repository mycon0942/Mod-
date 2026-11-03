package com.silentwatch;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * O Observador: fica parado ao longe, olhando para o jogador.
 * Nunca ataca. Some se voce encara por ~3s, se chega perto, ou com o tempo.
 * Se voce vira as costas por um tempo, volta a olhar e ele esta um pouco mais perto.
 */
public class EntityWatcher extends EntityLiving {
    private static final int MAX_AGE = 20 * 75;
    private static final double VANISH_DIST = 16.0D;
    private static final double CREEP_MIN_DIST = 24.0D;

    private int stareTicks;
    private int turnedAwayTicks;

    public EntityWatcher(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) return;

        EntityPlayer p = worldObj.getClosestPlayerToEntity(this, 160.0D);
        if (p == null || ticksExisted > MAX_AGE) {
            setDead();
            return;
        }

        double eyeY = p.boundingBox.minY + 1.62D;
        double dx = p.posX - posX;
        double dz = p.posZ - posZ;
        double dy = eyeY - (posY + 1.62D);
        double dist = Math.sqrt(dx * dx + dz * dz);

        // sempre virado para o jogador
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, dist) * 180.0D / Math.PI));
        rotationYaw = yaw;
        rotationYawHead = yaw;
        renderYawOffset = yaw;
        rotationPitch = pitch;

        if (dist < VANISH_DIST) {
            setDead();
            return;
        }

        // o jogador esta olhando para ele?
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vec3 look = p.getLook(1.0F);
        double dot = (look.xCoord * -dx + look.yCoord * -dy + look.zCoord * -dz) / len;

        if (dot > 0.985D && p.canEntityBeSeen(this)) {
            turnedAwayTicks = 0;
            if (++stareTicks > 60) {
                setDead();
            }
        } else {
            if (stareTicks > 0) stareTicks--;
            if (dot < 0.3D) {
                if (++turnedAwayTicks > 160 && dist > CREEP_MIN_DIST) {
                    creepCloser(dx, dz, dist);
                    turnedAwayTicks = 0;
                }
            } else {
                turnedAwayTicks = 0;
            }
        }
    }

    private void creepCloser(double dx, double dz, double dist) {
        double nx = posX + dx / dist * 2.5D;
        double nz = posZ + dz / dist * 2.5D;
        int ix = MathHelper.floor_double(nx);
        int iz = MathHelper.floor_double(nz);
        if (!worldObj.getChunkProvider().chunkExists(ix >> 4, iz >> 4)) return;
        int iy = worldObj.getTopSolidOrLiquidBlock(ix, iz);
        Block below = worldObj.getBlock(ix, iy - 1, iz);
        if (below.getMaterial().isLiquid() || below.isLeaves(worldObj, ix, iy - 1, iz)) return;
        if (Math.abs(iy - posY) > 3.0D) return;
        setPosition(nx, iy, nz);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        if (!worldObj.isRemote) setDead();
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    /** Nao e salvo no mundo: ao recarregar, ele simplesmente nao esta la. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }
}
