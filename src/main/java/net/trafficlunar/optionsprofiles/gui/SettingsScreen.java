package net.trafficlunar.optionsprofiles.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import net.trafficlunar.optionsprofiles.OptionsProfilesMod;

import java.io.IOException;

public class SettingsScreen extends GuiScreen {
    private final GuiScreen lastScreen;
    private boolean showProfilesButton;

    // Button IDs:
    // 1: Toggle show profiles button
    // 2: Done

    public SettingsScreen(GuiScreen lastScreen) {
        this.lastScreen = lastScreen;
        this.showProfilesButton = OptionsProfilesMod.config().shouldShowProfilesButton();
    }

    @Override
    public void initGui() {
        buttonList.add(new GuiButton(1, this.width / 2 - 75, this.height / 2 - 10, 150, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.show-profiles-button") + " "
                        + (showProfilesButton ? "§aON" : "§cOFF")));
        buttonList.add(new GuiButton(2, this.width / 2 - 100, this.height / 2 + 16, 200, 20, "Done"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                showProfilesButton = !showProfilesButton;
                OptionsProfilesMod.config().setShowProfilesButton(showProfilesButton);
                button.displayString = StatCollector.translateToLocal("gui.optionsprofiles.show-profiles-button") + " "
                        + (showProfilesButton ? "§aON" : "§cOFF");
                break;
            case 2:
                OptionsProfilesMod.config().save();
                this.mc.displayGuiScreen(lastScreen);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj,
                StatCollector.translateToLocal("gui.optionsprofiles.settings-menu"),
                this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
