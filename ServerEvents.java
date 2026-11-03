package com.silentwatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

public class ServerEvents {
    private final Random rand = new Random();
    private final Map<UUID, State> states = new HashMap<UUID, State>();

    private static class State {
        int watcherTimer, ambientTimer, steps, stepDelay;
        double sx, sz;
    }

    // ---------- Sem mobs ----------
    @SubscribeEvent
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn e) {
        EntityLivingBase l = e.entityLiving;
        if (l instanceof EntityWatcher) return;
        if (HorrorMod.blockHostile && l instanceof IMob) {
            e.setResult(Result.DENY);
        } else if (HorrorMod.blockPassive
                && (l instanceof EntityAnimal || l instanceof EntityAmbientCreature || l instanceof EntityWaterMob)) {
            e.setResult(Result.DENY);
        }
    }

    // ---------- Clima ----------
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != Side.SERVER) return;
        World w = e.world;
        if (w.provider.dimensionId != 0) return;
        WorldInfo info = w.getWorldInfo();
        if (HorrorMod.forceRain) {
            if (!info.isRaining()) info.setRaining(true);
            if (info.getRainTime() < 200) info.setRainTime(24000);
        }
        if (HorrorMod.forceThunder) {
            if (!info.isThundering()) info.setThundering(true);
            if (info.getThunderTime() < 200) info.setThunderTime(24000);
        }
        if (HorrorMod.duskLock) {
            w.setWorldTime(13500L);
        }
    }

    // ---------- Por jogador: sons e Observador ----------
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != Side.SERVER) return;
        EntityPlayer p = e.player;
        if (p.worldObj.provider.dimensionId != 0) return;

        State s = states.get(p.getUniqueID());
        if (s == null) {
            s = new State();
            s.watcherTimer = nextWatcherDelay();
            s.ambientTimer = 20 * (20 + rand.nextInt(30));
            states.put(p.getUniqueID(), s);
        }

        if (s.steps > 0) tickSteps(p, s);

        if (HorrorMod.ambientSounds && --s.ambientTimer <= 0) {
            ambient(p, s);
            s.ambientTimer = 20 * (25 + rand.nextInt(50));
        }

        if (HorrorMod.watcherEnabled && --s.watcherTimer <= 0) {
            s.watcherTimer = trySpawnWatcher(p) ? nextWatcherDelay() : 200;
        }
    }

    private int nextWatcherDelay() {
        int min = HorrorMod.watcherMinSeconds;
        int max = Math.max(min + 1, HorrorMod.watcherMaxSeconds);
        return 20 * (min + rand.nextInt(max - min));
    }

    private void ambient(EntityPlayer p, State s) {
        World w = p.worldObj;
        double yaw = Math.toRadians(p.rotationYaw);
        double a = rand.nextDouble() * Math.PI * 2;
        switch (rand.nextInt(4)) {
            case 0:
                w.playSoundEffect(p.posX, p.posY, p.posZ, "ambient.cave.cave", 0.7F, 0.7F);
                break;
            case 1: // passos se aproximando por tras
                if (s.steps == 0) {
                    s.steps = 6 + rand.nextInt(4);
                    s.stepDelay = 0;
                    s.sx = p.posX + Math.sin(yaw) * 10.0D;
                    s.sz = p.posZ - Math.cos(yaw) * 10.0D;
                }
                break;
            case 2: // porta ao longe
                w.playSoundEffect(p.posX + Math.cos(a) * 22, p.posY, p.posZ + Math.sin(a) * 22,
                        "random.door_close", 1.6F, 0.8F);
                break;
            default: // galho quebrando ao longe
                w.playSoundEffect(p.posX + Math.cos(a) * 18, p.posY, p.posZ + Math.sin(a) * 18,
                        "dig.wood", 1.5F, 0.6F);
                break;
        }
    }

    /** Passos que chegam perto... e param. */
    private void tickSteps(EntityPlayer p, State s) {
        if (--s.stepDelay > 0) return;
        s.stepDelay = 11 + rand.nextInt(3);
        double dx = p.posX - s.sx;
        double dz = p.posZ - s.sz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 4.0D || s.steps-- <= 0) {
            s.steps = 0;
            return;
        }
        s.sx += dx / d * 0.9D;
        s.sz += dz / d * 0.9D;
        World w = p.worldObj;
        int ix = MathHelper.floor_double(s.sx);
        int iz = MathHelper.floor_double(s.sz);
        int iy = w.getTopSolidOrLiquidBlock(ix, iz);
        Block b = w.getBlock(ix, iy - 1, iz);
        w.playSoundEffect(s.sx, iy, s.sz, b.stepSound.getStepResourcePath(), 0.7F, 0.9F + rand.nextFloat() * 0.1F);
    }

    private boolean trySpawnWatcher(EntityPlayer p) {
        World w = p.worldObj;
        if (!w.getEntitiesWithinAABB(EntityWatcher.class, p.boundingBox.expand(100, 100, 100)).isEmpty()) {
            return false;
        }
        double eyeY = p.boundingBox.minY + 1.62D;
        for (int i = 0; i < 16; i++) {
            double dist = 30.0D + rand.nextDouble() * 14.0D;
            double off = (25.0D + rand.nextDouble() * 45.0D) * (rand.nextBoolean() ? 1 : -1);
            double a = Math.toRadians(p.rotationYaw + off); // na borda do campo de visao
            double x = p.posX - Math.sin(a) * dist;
            double z = p.posZ + Math.cos(a) * dist;
            int ix = MathHelper.floor_double(x);
            int iz = MathHelper.floor_double(z);
            if (!w.getChunkProvider().chunkExists(ix >> 4, iz >> 4)) continue;
            int iy = w.getTopSolidOrLiquidBlock(ix, iz);
            Block below = w.getBlock(ix, iy - 1, iz);
            if (below.getMaterial().isLiquid() || below.isLeaves(w, ix, iy - 1, iz)) continue;
            if (!w.isAirBlock(ix, iy, iz) || !w.isAirBlock(ix, iy + 1, iz)) continue;
            Vec3 from = Vec3.createVectorHelper(p.posX, eyeY, p.posZ);
            Vec3 to = Vec3.createVectorHelper(x, iy + 1.6D, z);
            if (w.rayTraceBlocks(from, to) != null) continue; // precisa de linha de visao
            EntityWatcher watcher = new EntityWatcher(w);
            watcher.setLocationAndAngles(ix + 0.5D, iy, iz + 0.5D, 0.0F, 0.0F);
            w.spawnEntityInWorld(watcher);
            return true;
        }
        return false;
    }
}
