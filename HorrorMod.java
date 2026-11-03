package com.silentwatch;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

@Mod(modid = HorrorMod.MODID, name = "Silent Watch", version = "1.0")
public class HorrorMod {
    public static final String MODID = "silentwatch";

    @Instance(MODID)
    public static HorrorMod instance;

    @SidedProxy(clientSide = "com.silentwatch.ClientProxy", serverSide = "com.silentwatch.CommonProxy")
    public static CommonProxy proxy;

    // Config
    public static boolean blockHostile, blockPassive, forceRain, forceThunder, duskLock;
    public static boolean thickFog, ambientSounds, watcherEnabled;
    public static float fogDensity;
    public static int watcherMinSeconds, watcherMaxSeconds;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration c = new Configuration(e.getSuggestedConfigurationFile());
        c.load();
        blockHostile = c.getBoolean("blockHostile", "spawns", true, "Impede spawn natural de monstros");
        blockPassive = c.getBoolean("blockPassive", "spawns", true, "Impede spawn natural de animais (mundo vazio e silencioso)");
        forceRain = c.getBoolean("forceRain", "weather", true, "Mantem chuva constante no Overworld");
        forceThunder = c.getBoolean("forceThunder", "weather", false, "Mantem tempestade com trovao");
        duskLock = c.getBoolean("duskLock", "weather", false, "Trava o horario num crepusculo sombrio");
        thickFog = c.getBoolean("thickFog", "visual", true, "Nevoa pesada e acinzentada");
        fogDensity = c.getFloat("fogDensity", "visual", 0.045F, 0.005F, 0.3F, "Densidade da nevoa (maior = mais fechada)");
        ambientSounds = c.getBoolean("ambientSounds", "audio", true, "Passos atras de voce, portas distantes, galhos");
        watcherEnabled = c.getBoolean("watcherEnabled", "watcher", true, "O Observador (criatura com sua aparencia)");
        watcherMinSeconds = c.getInt("minSeconds", "watcher", 120, 20, 3600, "Tempo minimo entre aparicoes");
        watcherMaxSeconds = c.getInt("maxSeconds", "watcher", 360, 30, 7200, "Tempo maximo entre aparicoes");
        if (c.hasChanged()) c.save();
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        EntityRegistry.registerModEntity(EntityWatcher.class, "Watcher", 0, this, 160, 2, false);

        ServerEvents se = new ServerEvents();
        MinecraftForge.EVENT_BUS.register(se);
        FMLCommonHandler.instance().bus().register(se);

        proxy.registerClient();
    }
}
