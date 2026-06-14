package net.trafficlunar.optionsprofiles;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import net.trafficlunar.optionsprofiles.profiles.ProfileConfiguration;
import net.trafficlunar.optionsprofiles.profiles.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Keybinds {
    private static final KeyBinding[] PROFILE_KEYMAPPINGS = new KeyBinding[3];

    public static void init() {
        for (int i = 0; i < PROFILE_KEYMAPPINGS.length; i++) {
            PROFILE_KEYMAPPINGS[i] = new KeyBinding(
                    "key.optionsprofiles.profile_" + (i + 1),
                    0,  // unbound by default
                    "key.categories.optionsprofiles"
            );
            ClientRegistry.registerKeyBinding(PROFILE_KEYMAPPINGS[i]);
        }
    }

    public static void onTick() {
        for (int i = 0; i < PROFILE_KEYMAPPINGS.length; i++) {
            final int keybindIndex = i + 1;
            while (PROFILE_KEYMAPPINGS[i].isPressed()) {
                loadProfilesByKeybind(keybindIndex);
            }
        }
    }

    private static void loadProfilesByKeybind(int keybindIndex) {
        try (Stream<Path> paths = Files.list(Profiles.PROFILES_DIRECTORY)) {
            paths.filter(Files::isDirectory).forEach(path -> {
                String profileName = path.getFileName().toString();
                ProfileConfiguration cfg = ProfileConfiguration.get(profileName);
                if (cfg.getKeybindIndex() == keybindIndex) {
                    Profiles.loadProfile(profileName);
                    OptionsProfilesMod.LOGGER.info("[Profile '{}']: Loaded through keybind {}", profileName, keybindIndex);
                }
            });
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("Error loading profiles through keybind {}", keybindIndex, e);
        }
    }
}
