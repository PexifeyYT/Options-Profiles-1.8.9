package net.trafficlunar.optionsprofiles.profiles;

import net.minecraft.client.Minecraft;
import net.trafficlunar.optionsprofiles.OptionsProfilesMod;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Profiles {
    public static final Path PROFILES_DIRECTORY = Paths.get("options-profiles");
    public static final Path OPTIONS_FILE = Paths.get("options.txt");
    public static final Path OPTIFINE_OPTIONS_FILE = Paths.get("optionsof.txt");

    public static void createProfile() {
        String profileName = "Profile 1";
        Path profile = PROFILES_DIRECTORY.resolve(profileName);

        for (int i = 2; Files.exists(profile); i++) {
            profileName = "Profile " + i;
            profile = PROFILES_DIRECTORY.resolve(profileName);
        }

        try {
            Files.createDirectory(profile);
            OptionsProfilesMod.LOGGER.info("[Profile '{}']: created", profileName);
            writeProfile(profileName, false);
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("[Profile '{}']: An error occurred when creating a profile", profileName, e);
        }
    }

    private static void copyOptionFile(Path profile, Path options) {
        if (Files.exists(options)) {
            Path dest = profile.resolve(options.getFileName());
            try {
                Files.copy(options, dest);
                OptionsProfilesMod.LOGGER.info("[Profile '{}']: Copied file '{}'",
                        profile.getFileName().toString(), options.getFileName().toString());
            } catch (IOException e) {
                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Unable to copy '{}'",
                        profile.getFileName().toString(), options.getFileName().toString(), e);
            }
        }
    }

    public static void writeProfile(String profileName, boolean overwriting) {
        Path profile = PROFILES_DIRECTORY.resolve(profileName);
        Path profileOptions = profile.resolve("options.txt");

        if (overwriting) {
            try (Stream<Path> files = Files.list(profile)) {
                files.filter(file -> !file.getFileName().toString().equals("configuration.json"))
                        .forEach(file -> {
                            try {
                                Files.delete(file);
                                OptionsProfilesMod.LOGGER.info("[Profile '{}']: Deleted file '{}'", profileName, file.getFileName().toString());
                            } catch (IOException e) {
                                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error deleting '{}'", profileName, file.getFileName().toString(), e);
                            }
                        });
            } catch (IOException e) {
                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error deleting old options files.", profileName, e);
            }
        }

        copyOptionFile(profile, OPTIONS_FILE);
        copyOptionFile(profile, OPTIFINE_OPTIONS_FILE);

        if (!overwriting) {
            ProfileConfiguration profileConfiguration = ProfileConfiguration.get(profileName);
            try (Stream<String> lines = Files.lines(profileOptions)) {
                List<String> optionsToLoad = profileConfiguration.getOptionsToLoad();
                lines.forEach(line -> {
                    String[] option = line.split(":", 2);
                    if (!option[0].isEmpty()) {
                        optionsToLoad.add(option[0]);
                    }
                });
                profileConfiguration.save();
            } catch (IOException e) {
                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error adding options to configuration", profileName, e);
            }
        }
    }

    private static void loadOptionFile(String profileName, Path options) {
        ProfileConfiguration profileConfiguration = ProfileConfiguration.get(profileName);
        Path profile = PROFILES_DIRECTORY.resolve(profileName);
        Path profileOptions = profile.resolve(options.getFileName());

        if (!Files.exists(profileOptions)) return;

        if (options.getFileName().toString().equals("options.txt")) {
            Map<String, String> optionsToWrite = new HashMap<>();

            // Read current options.txt
            try (Stream<String> lines = Files.lines(options)) {
                lines.forEach(line -> {
                    String[] option = line.split(":", 2);
                    optionsToWrite.put(option[0], option.length > 1 ? option[1] : "");
                });
            } catch (IOException e) {
                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error reading options.txt", profileName, e);
            }

            // Overwrite with profile values for selected options
            try (Stream<String> lines = Files.lines(profileOptions)) {
                lines.forEach(line -> {
                    String[] option = line.split(":", 2);
                    if (option.length > 1 && profileConfiguration.getOptionsToLoad().contains(option[0])) {
                        optionsToWrite.put(option[0], option[1]);
                    }
                });
            } catch (IOException e) {
                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error reading profile options.txt", profileName, e);
            }

            // Write merged result
            try {
                Files.write(options, () ->
                        optionsToWrite.entrySet().stream()
                                .<CharSequence>map(entry -> entry.getKey() + ":" + entry.getValue())
                                .iterator()
                );
                OptionsProfilesMod.LOGGER.info("[Profile '{}']: options.txt loaded with specific options", profileName);
            } catch (IOException e) {
                OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error writing options.txt", profileName, e);
            }
            return;
        }

        // For non-options.txt files (e.g. optionsof.txt), just copy
        try {
            Files.copy(profileOptions, options, StandardCopyOption.REPLACE_EXISTING);
            OptionsProfilesMod.LOGGER.info("[Profile '{}']: '{}' loaded by copying", profileName, options.getFileName());
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error loading '{}'", profileName, options.getFileName(), e);
        }
    }

    public static void loadProfile(String profileName) {
        loadOptionFile(profileName, OPTIONS_FILE);
        loadOptionFile(profileName, OPTIFINE_OPTIONS_FILE);

        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.loadOptions();
        mc.gameSettings.saveOptions();

        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    public static void renameProfile(String profileName, String newProfileName) {
        if (profileName.equals(newProfileName) || newProfileName.trim().isEmpty()) return;

        Path profile = PROFILES_DIRECTORY.resolve(profileName);
        Path newProfile = PROFILES_DIRECTORY.resolve(newProfileName.trim());

        if (Files.exists(newProfile)) {
            OptionsProfilesMod.LOGGER.warn("[Profile '{}']: A profile with that name already exists!", profileName);
            return;
        }

        try {
            Files.move(profile, newProfile);
            OptionsProfilesMod.LOGGER.info("[Profile '{}']: renamed to '{}'", profileName, newProfileName);
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("[Profile '{}']: Error renaming profile", profileName, e);
        }
    }

    public static void deleteProfile(String profileName) {
        Path profile = PROFILES_DIRECTORY.resolve(profileName);
        try {
            FileUtils.deleteDirectory(profile.toFile());
            OptionsProfilesMod.LOGGER.info("[Profile '{}']: deleted", profileName);
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("[Profile '{}']: Profile was not deleted", profileName, e);
        }
    }
}
