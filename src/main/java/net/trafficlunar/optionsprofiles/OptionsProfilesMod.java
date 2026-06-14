package net.trafficlunar.optionsprofiles;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod(modid = OptionsProfilesMod.MOD_ID, name = "Options Profiles", version = "1.4.4")
public class OptionsProfilesMod {
    public static final String MOD_ID = "optionsprofiles";
    public static final Logger LOGGER = LogManager.getLogger("Options Profiles");

    @Mod.Instance(OptionsProfilesMod.MOD_ID)
    public static OptionsProfilesMod instance;

    private static OptionsProfilesModConfiguration CONFIG;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Create options-profiles directory
        Path profilesDirectory = Paths.get("options-profiles");
        if (Files.notExists(profilesDirectory)) {
            try {
                Files.createDirectory(profilesDirectory);
            } catch (IOException e) {
                LOGGER.error("An error occurred when creating the 'options-profiles' directory.", e);
            }
        }

        // Load mod config
        CONFIG = OptionsProfilesModConfiguration.load();

        // Register event handlers on both buses
        ForgeEventHandler handler = new ForgeEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);

        // Register keybinds and commands
        Keybinds.init();
        Commands.init();
    }

    public static OptionsProfilesModConfiguration config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }
}
