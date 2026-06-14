package net.trafficlunar.optionsprofiles;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.trafficlunar.optionsprofiles.gui.ProfilesScreen;
import net.trafficlunar.optionsprofiles.profiles.ProfileConfiguration;
import net.trafficlunar.optionsprofiles.profiles.Profiles;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class ForgeEventHandler {
    private static final int PROFILES_BUTTON_ID = 250;
    private static boolean startupProfilesLoaded = false;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiOptions) {
            if (OptionsProfilesMod.config().shouldShowProfilesButton()) {
                event.buttonList.add(new GuiButton(PROFILES_BUTTON_ID, 5, 5, 75, 20, "Profiles"));
            }
        }
    }

    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.gui instanceof GuiOptions && event.button.id == PROFILES_BUTTON_ID) {
            event.gui.mc.displayGuiScreen(new ProfilesScreen(event.gui));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!startupProfilesLoaded && event.gui instanceof GuiMainMenu) {
            startupProfilesLoaded = true;
            loadStartupProfiles();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Keybinds.onTick();
        }
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

                String[] serverList = servers.split(",");
                for (String s : serverList) {
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
