package net.trafficlunar.optionsprofiles.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;
import net.trafficlunar.optionsprofiles.OptionsProfilesMod;
import net.trafficlunar.optionsprofiles.profiles.ProfileConfiguration;
import net.trafficlunar.optionsprofiles.profiles.Profiles;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionsToggleScreen extends GuiScreen {
    private final GuiScreen lastScreen;
    private final String profileName;
    private final ProfileConfiguration profileConfiguration;

    private GuiTextField searchField;
    private final List<String[]> allOptions = new ArrayList<>();   // [key, value]
    private List<String[]> filteredOptions = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE = 10;
    private static final int LIST_TOP = 46;
    private static final int ENTRY_HEIGHT = 20;

    // Button IDs:
    // 0-9  : Toggle buttons for visible slots
    // 20   : All OFF
    // 21   : All ON
    // 22   : Done
    // 23   : Scroll Up
    // 24   : Scroll Down

    public OptionsToggleScreen(GuiScreen lastScreen, String profileName, ProfileConfiguration profileConfiguration) {
        this.lastScreen = lastScreen;
        this.profileName = profileName;
        this.profileConfiguration = profileConfiguration;
    }

    @Override
    public void initGui() {
        searchField = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 100, 24, 200, 16);
        searchField.setMaxStringLength(64);

        loadOptions();

        buttonList.add(new GuiButton(20, this.width / 2 - 155, this.height - 28, 75, 20,
                "§c" + StatCollector.translateToLocal("gui.optionsprofiles.all-off")));
        buttonList.add(new GuiButton(21, this.width / 2 - 78, this.height - 28, 75, 20,
                "§a" + StatCollector.translateToLocal("gui.optionsprofiles.all-on")));
        buttonList.add(new GuiButton(22, this.width / 2 + 5, this.height - 28, 150, 20, "Done"));

        refreshEntryButtons();
    }

    private void loadOptions() {
        allOptions.clear();
        Path profile = Profiles.PROFILES_DIRECTORY.resolve(profileName);
        Path optionsFile = profile.resolve("options.txt");

        try (Stream<String> lines = Files.lines(optionsFile)) {
            lines.forEach(line -> {
                String[] parts = line.split(":", 2);
                if (parts.length >= 1 && !parts[0].isEmpty()) {
                    allOptions.add(new String[]{parts[0], parts.length > 1 ? parts[1] : ""});
                }
            });
        } catch (IOException e) {
            OptionsProfilesMod.LOGGER.error("Error loading options for toggle screen", e);
        }

        applyFilter();
    }

    private void applyFilter() {
        String filter = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        if (filter.isEmpty()) {
            filteredOptions = new ArrayList<>(allOptions);
        } else {
            filteredOptions = allOptions.stream()
                    .filter(opt -> opt[0].toLowerCase().contains(filter))
                    .collect(Collectors.toList());
        }
        scrollOffset = 0;
        refreshEntryButtons();
    }

    @SuppressWarnings("unchecked")
    private void refreshEntryButtons() {
        buttonList.removeIf(b -> (b.id >= 0 && b.id <= 9) || b.id == 23 || b.id == 24);

        int toggleX = this.width / 2 + 90;

        for (int slot = 0; slot < MAX_VISIBLE; slot++) {
            int optIdx = slot + scrollOffset;
            if (optIdx >= filteredOptions.size()) break;

            String key = filteredOptions.get(optIdx)[0];
            boolean toggled = profileConfiguration.getOptionsToLoad().contains(key);
            int y = LIST_TOP + slot * ENTRY_HEIGHT;

            buttonList.add(new GuiButton(slot, toggleX, y, 44, 18,
                    toggled ? "§aON" : "§cOFF"));
        }

        if (filteredOptions.size() > MAX_VISIBLE) {
            buttonList.add(new GuiButton(23, this.width - 23, LIST_TOP, 20, 20, "^"));
            buttonList.add(new GuiButton(24, this.width - 23, this.height - 50, 20, 20, "v"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        if (id >= 0 && id <= 9) {
            int optIdx = id + scrollOffset;
            if (optIdx < filteredOptions.size()) {
                String key = filteredOptions.get(optIdx)[0];
                List<String> toLoad = profileConfiguration.getOptionsToLoad();
                if (toLoad.contains(key)) {
                    toLoad.remove(key);
                    button.displayString = "§cOFF";
                } else {
                    toLoad.add(key);
                    button.displayString = "§aON";
                }
                profileConfiguration.setOptionsToLoad(toLoad);
            }
        } else {
            switch (id) {
                case 20: // All OFF
                    profileConfiguration.setOptionsToLoad(new ArrayList<>());
                    refreshEntryButtons();
                    break;
                case 21: // All ON
                    List<String> allKeys = allOptions.stream()
                            .map(o -> o[0]).collect(Collectors.toList());
                    profileConfiguration.setOptionsToLoad(allKeys);
                    refreshEntryButtons();
                    break;
                case 22: // Done
                    profileConfiguration.save();
                    this.mc.displayGuiScreen(lastScreen);
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
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta,
                Math.max(0, filteredOptions.size() - MAX_VISIBLE)));
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
                StatCollector.translateToLocal("gui.optionsprofiles.options-toggle") + ": " + profileName,
                this.width / 2, 8, 0xFFFFFF);

        searchField.drawTextBox();

        // Draw option keys and values
        for (int slot = 0; slot < MAX_VISIBLE; slot++) {
            int optIdx = slot + scrollOffset;
            if (optIdx >= filteredOptions.size()) break;

            String key = filteredOptions.get(optIdx)[0];
            String value = filteredOptions.get(optIdx)[1];
            int y = LIST_TOP + slot * ENTRY_HEIGHT;

            this.fontRendererObj.drawStringWithShadow(key, 10, y + 5, 0xFFFFFF);

            // Show value as greyed-out text if there's space
            int keyWidth = this.fontRendererObj.getStringWidth(key);
            int maxX = this.width / 2 + 85;
            if (keyWidth + 20 < maxX && !value.isEmpty()) {
                String truncValue = "§7= " + value;
                int valX = 10 + keyWidth + 6;
                if (valX + this.fontRendererObj.getStringWidth(truncValue) > maxX) {
                    // Truncate if needed
                    truncValue = "§7=..";
                }
                this.fontRendererObj.drawStringWithShadow(truncValue, valX, y + 5, 0xFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        String before = searchField.getText();
        searchField.textboxKeyTyped(typedChar, keyCode);
        if (!searchField.getText().equals(before)) {
            applyFilter();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
