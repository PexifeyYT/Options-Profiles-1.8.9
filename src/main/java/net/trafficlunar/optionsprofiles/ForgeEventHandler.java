package net.trafficlunar.optionsprofiles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.trafficlunar.optionsprofiles.gui.ProfilesScreen;
import net.trafficlunar.optionsprofiles.profiles.ProfileConfiguration;
import net.trafficlunar.optionsprofiles.profiles.Profiles;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ForgeEventHandler {
    private static final int PROFILES_BUTTON_ID = 250;
    private static boolean startupProfilesLoaded = false;

    /** Walk up class hierarchy to find buttonList — handles Lunar's GuiOptions subclass */
    @SuppressWarnings("unchecked")
    private static List<GuiButton> getButtonList(GuiScreen screen) {
        Class<?> cls = screen.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField("buttonList");
                f.setAccessible(true);
                return (List<GuiButton>) f.get(screen);
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** True if screen is GuiOptions or any subclass/reimplementation by name */
    private static boolean isOptionsScreen(GuiScreen screen) {
        if (screen == null) return false;
        Class<?> cls = screen.getClass();
        while (cls != null && cls != Object.class) {
            String name = cls.getSimpleName();
            if (name.equals("GuiOptions")) return true;
            cls = cls.getSuperclass();
        }
        return false;
    }

    /** True if screen is GuiMainMenu or subclass by name */
    private static boolean isMainMenuScreen(GuiScreen screen) {
        if (screen == null) return false;
        Class<?> cls = screen.getClass();
        while (cls != null && cls != Object.class) {
            String name = cls.getSimpleName();
            if (name.equals("GuiMainMenu")) return true;
            cls = cls.getSuperclass();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc.currentScreen;

        // Startup profile detection — tick-based, name-checked for Lunar compatibility
        if (!startupProfilesLoaded && isMainMenuScreen(screen)) {
            startupProfilesLoaded = true;
            loadStartupProfiles();
        }

        // Inject Profiles button — name-based check so Lunar's GuiOptions subclass matches
        if (isOptionsScreen(screen) && OptionsProfilesMod.config().shouldShowProfilesButton()) {
            List<GuiButton> buttons = getButtonList(screen);
            if (buttons == null) return;

            boolean found = false;
            for (GuiButton btn : buttons) {
                if (btn.id == PROFILES_BUTTON_ID) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                final GuiScreen optionsGui = screen;
                buttons.add(new GuiButton(PROFILES_BUTTON_ID, 5, 5, 75, 20, "Profiles") {
                    @Override
                    public boolean mousePressed(Minecraft mc2, int mouseX, int mouseY) {
                        boolean hit = super.mousePressed(mc2, mouseX, mouseY);
                        if (hit) {
                            mc2.displayGuiScreen(new ProfilesScreen(optionsGui));
                        }
                        return hit;
                    }
                });
            }
        }

        Keybinds.onTick();
    }

    private void loadStartupProfiles() {
        try (Stream<Path> paths = Files.list(Profiles.PROFILES_DIRECTORY)) {
            paths.filter(Files::isDirectory).forEach(path -> {
                String profileName = path.getFileName().toString();
                ProfileConfiguration cfg = ProfileConfiguration.get(profileName);
                if (cfg.shouldLoadOnStartup()) {
                    Profiles.loadProfile(profileName);
                    OptionsProfilesMod.LOGGER.info("[Profile '{}']: Loaded on startup", profileName);
                }
            });
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("Error loading startup profiles", e);
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event.manager == null || event.manager.isLocalChannel()) return;

        SocketAddress addr = event.manager.getRemoteAddress();
        if (addr instanceof InetSocketAddress) {
            String ip = ((InetSocketAddress) addr).getHostString().trim();
            handleServerEvent(ip, false);
        }
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (event.manager == null || event.manager.isLocalChannel()) return;
        handleServerEvent("leave", true);
    }

    private void handleServerEvent(String ip, boolean isLeave) {
        try (Stream<Path> paths = Files.list(Profiles.PROFILES_DIRECTORY)) {
            paths.filter(Files::isDirectory).forEach(path -> {
                String profileName = path.getFileName().toString();
                ProfileConfiguration cfg = ProfileConfiguration.get(profileName);
                String servers = cfg.getServers();
                if (servers == null || servers.isEmpty()) return;

                for (String s : servers.split(",")) {
                    if (s.trim().equals(ip)) {
                        Profiles.loadProfile(profileName);
                        OptionsProfilesMod.LOGGER.info("[Profile '{}']: Loaded on server ({}){}", profileName, ip, isLeave ? " leave" : "");
                        break;
                    }
                }
            });
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("Error handling server event for profiles", e);
        }
    }
}
