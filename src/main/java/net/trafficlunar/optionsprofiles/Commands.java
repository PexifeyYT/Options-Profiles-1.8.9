package net.trafficlunar.optionsprofiles;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.trafficlunar.optionsprofiles.gui.ProfilesScreen;

import java.util.Collections;
import java.util.List;

public class Commands {
    public static void init() {
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return "optionsprofiles";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/optionsprofiles";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                Minecraft mc = Minecraft.getMinecraft();
                mc.displayGuiScreen(new ProfilesScreen(null));
            }

            @Override
            public int getRequiredPermissionLevel() {
                return 0;
            }

            @Override
            public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
                return Collections.emptyList();
            }
        });
    }
}
