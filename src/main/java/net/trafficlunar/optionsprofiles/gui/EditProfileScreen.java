package net.trafficlunar.optionsprofiles.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;
import net.trafficlunar.optionsprofiles.profiles.ProfileConfiguration;
import net.trafficlunar.optionsprofiles.profiles.Profiles;

import java.io.IOException;

public class EditProfileScreen extends GuiScreen {
    private final ProfilesScreen profilesScreen;
    private final String originalProfileName;
    private final ProfileConfiguration profileConfiguration;

    private GuiTextField profileNameField;
    private GuiTextField serversField;
    private int keybindIndex;
    private boolean loadOnStartup;

    // Button IDs:
    // 1: Overwrite
    // 2: Options toggle
    // 3: Cycle keybind index
    // 4: Toggle load on startup
    // 5: Done
    // 6: Delete

    public EditProfileScreen(ProfilesScreen profilesScreen, String profileName) {
        this.profilesScreen = profilesScreen;
        this.originalProfileName = profileName;
        this.profileConfiguration = ProfileConfiguration.get(profileName);
        this.keybindIndex = profileConfiguration.getKeybindIndex();
        this.loadOnStartup = profileConfiguration.shouldLoadOnStartup();
    }

    @Override
    public void initGui() {
        int cx = this.width / 2;

        profileNameField = new GuiTextField(0, this.fontRendererObj, cx - 102, 60, 204, 20);
        profileNameField.setMaxStringLength(50);
        profileNameField.setText(originalProfileName);
        profileNameField.setFocused(true);

        serversField = new GuiTextField(1, this.fontRendererObj, cx - 102, 92, 204, 20);
        serversField.setMaxStringLength(256);
        serversField.setText(profileConfiguration.getServers());

        buttonList.add(new GuiButton(1, cx - 75, 120, 150, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.overwrite-options")));
        buttonList.add(new GuiButton(2, cx - 75, 144, 150, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.options-toggle") + "..."));
        buttonList.add(new GuiButton(3, cx - 75, 168, 150, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.keybind-index") + " " + keybindIndex));
        buttonList.add(new GuiButton(4, cx - 75, 192, 150, 20,
                StatCollector.translateToLocal("gui.optionsprofiles.load-on-startup") + " " + (loadOnStartup ? "§aON" : "§cOFF")));
        buttonList.add(new GuiButton(5, cx - 100, this.height - 28, 200, 20, "Done"));
        buttonList.add(new GuiButton(6, this.width - 60, this.height - 28, 55, 20, "§cDelete"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1: // Overwrite
                Profiles.writeProfile(originalProfileName, true);
                closeWithSave(false);
                break;
            case 2: // Options Toggle
                saveCurrentState();
                this.mc.displayGuiScreen(new OptionsToggleScreen(this, originalProfileName, profileConfiguration));
                break;
            case 3: // Cycle keybind index (0,1,2,3)
                keybindIndex = (keybindIndex + 1) % 4;
                button.displayString = StatCollector.translateToLocal("gui.optionsprofiles.keybind-index") + " " + keybindIndex;
                profileConfiguration.setKeybindIndex(keybindIndex);
                break;
            case 4: // Toggle load on startup
                loadOnStartup = !loadOnStartup;
                button.displayString = StatCollector.translateToLocal("gui.optionsprofiles.load-on-startup") + " "
                        + (loadOnStartup ? "§aON" : "§cOFF");
                profileConfiguration.setLoadOnStartup(loadOnStartup);
                break;
            case 5: // Done
                closeWithSave(false);
                break;
            case 6: // Delete
                Profiles.deleteProfile(originalProfileName);
                closeWithSave(true);
                break;
        }
    }

    private void saveCurrentState() {
        profileConfiguration.setServers(serversField.getText());
        profileConfiguration.setKeybindIndex(keybindIndex);
        profileConfiguration.setLoadOnStartup(loadOnStartup);
        profileConfiguration.save();
    }

    private void closeWithSave(boolean deleted) {
        if (!deleted) {
            saveCurrentState();
            String newName = profileNameField.getText().trim();
            if (!newName.isEmpty() && !newName.equals(originalProfileName)) {
                Profiles.renameProfile(originalProfileName, newName);
            }
        }
        profilesScreen.refreshList();
        this.mc.displayGuiScreen(profilesScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int cx = this.width / 2;

        this.drawCenteredString(this.fontRendererObj,
                StatCollector.translateToLocal("gui.optionsprofiles.editing-profile-title") + " " + originalProfileName,
                cx, 12, 0xFFFFFF);

        this.fontRendererObj.drawStringWithShadow(
                StatCollector.translateToLocal("gui.optionsprofiles.profile-name-text"),
                cx - 102, 50, 0xA0A0A0);
        this.fontRendererObj.drawStringWithShadow(
                StatCollector.translateToLocal("gui.optionsprofiles.servers-text"),
                cx - 102, 82, 0xA0A0A0);

        profileNameField.drawTextBox();
        serversField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        profileNameField.textboxKeyTyped(typedChar, keyCode);
        serversField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        profileNameField.mouseClicked(mouseX, mouseY, mouseButton);
        serversField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
