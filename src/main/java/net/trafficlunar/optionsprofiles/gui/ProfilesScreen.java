package net.trafficlunar.optionsprofiles.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import net.trafficlunar.optionsprofiles.OptionsProfilesMod;
import net.trafficlunar.optionsprofiles.profiles.Profiles;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProfilesScreen extends GuiScreen {
    private final GuiScreen lastScreen;
    private final List<String> profiles = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE = 8;
    private static final int ENTRY_HEIGHT = 22;
    private static final int LIST_TOP = 30;

    // Button ID ranges:
    // 0-7   : Edit buttons per visible slot
    // 10-17 : Load buttons per visible slot
    // 20    : Save Current Options
    // 21    : Done
    // 22    : Settings
    // 23    : Scroll Up
    // 24    : Scroll Down

    public ProfilesScreen(GuiScreen lastScreen) {
        this.lastScreen = lastScreen;
    }

    @Override
    public void initGui() {
        refreshProfiles();

        buttonList.add(new GuiButton(20, this.width / 2 - 155, this.height - 28, 150, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.save-current-options")));
        buttonList.add(new GuiButton(21, this.width / 2 + 5, this.height - 28, 150, 20, "Done"));
        buttonList.add(new GuiButton(22, 5, 5, 75, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.settings-button")));

        refreshEntryButtons();
    }

    public void refreshList() {
        refreshProfiles();
        refreshEntryButtons();
    }

    private void refreshProfiles() {
        profiles.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Profiles.PROFILES_DIRECTORY)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    profiles.add(p.getFileName().toString());
                }
            }
            profiles.sort(Comparator.naturalOrder());
        } catch (Exception e) {
            OptionsProfilesMod.LOGGER.error("Error listing profiles", e);
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, profiles.size() - MAX_VISIBLE)));
    }

    @SuppressWarnings("unchecked")
    private void refreshEntryButtons() {
        buttonList.removeIf(b -> (b.id >= 0 && b.id <= 17) || b.id == 23 || b.id == 24);

        int editBtnX = this.width / 2 + 30;
        int loadBtnX = editBtnX + 78;

        for (int slot = 0; slot < MAX_VISIBLE; slot++) {
            int profileIdx = slot + scrollOffset;
            if (profileIdx >= profiles.size()) break;

            int y = LIST_TOP + slot * ENTRY_HEIGHT;
            buttonList.add(new GuiButton(slot, editBtnX, y, 75, 20,
                    StatCollector.translateToLocal("gui.optionsprofiles.edit-profile")));
            buttonList.add(new GuiButton(slot + 10, loadBtnX, y, 75, 20,
                    StatCollector.translateToLocal("gui.optionsprofiles.load-profile")));
        }

        if (profiles.size() > MAX_VISIBLE) {
            buttonList.add(new GuiButton(23, this.width - 23, LIST_TOP, 20, 20, "^"));
            buttonList.add(new GuiButton(24, this.width - 23, this.height - 50, 20, 20, "v"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        if (id >= 0 && id <= 7) {
            // Edit for slot id
            int profileIdx = id + scrollOffset;
            if (profileIdx < profiles.size()) {
                this.mc.displayGuiScreen(new EditProfileScreen(this, profiles.get(profileIdx)));
            }
        } else if (id >= 10 && id <= 17) {
            // Load for slot (id - 10)
            int profileIdx = (id - 10) + scrollOffset;
            if (profileIdx < profiles.size()) {
                String name = profiles.get(profileIdx);
                Profiles.loadProfile(name);
                OptionsProfilesMod.LOGGER.info("[Profile '{}']: Loaded through button", name);
            }
        } else {
            switch (id) {
                case 20: // Save Current Options
                    Profiles.createProfile();
                    refreshProfiles();
                    refreshEntryButtons();
                    break;
                case 21: // Done
                    this.mc.displayGuiScreen(lastScreen);
                    break;
                case 22: // Settings
                    this.mc.displayGuiScreen(new SettingsScreen(this));
                    break;
                case 23: // Scroll Up
                    scroll(-1);
                    break;
                case 24: // Scroll Down
                    scroll(1);
                    break;
            }
        }
    }

    private void scroll(int delta) {
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, Math.max(0, profiles.size() - MAX_VISIBLE)));
        refreshEntryButtons();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll(wheel > 0 ? -1 : 1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.drawCenteredString(this.fontRendererObj,
                StatCollector.translateToLocal("gui.optionsprofiles.profiles-menu"),
                this.width / 2, 12, 0xFFFFFF);

        // Draw profile names for visible slots
        for (int slot = 0; slot < MAX_VISIBLE; slot++) {
            int profileIdx = slot + scrollOffset;
            if (profileIdx >= profiles.size()) break;
            int y = LIST_TOP + slot * ENTRY_HEIGHT;
            this.fontRendererObj.drawStringWithShadow(profiles.get(profileIdx), 10, y + 6, 0xFFFFFF);
        }

        // Scroll indicator
        if (profiles.size() > MAX_VISIBLE) {
            String indicator = (scrollOffset + 1) + "/" + (profiles.size() - MAX_VISIBLE + 1);
            this.fontRendererObj.drawStringWithShadow(indicator, this.width - 50, LIST_TOP + 3, 0xAAAAAA);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
