package it.frafol.cleanss.bungee.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import it.frafol.cleanss.bungee.enums.BungeeConfig;
import it.frafol.cleanss.bungee.enums.BungeeMessages;
import it.frafol.cleanss.bungee.objects.TextFile;
import it.frafol.cleanss.bungee.objects.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("ssreload","","screensharereload","cleanssreload","cleanscreensharereload");
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void execute(@NotNull CommandSender sender, String[] args) {

        if (sender.hasPermission(BungeeConfig.RELOAD_PERMISSION.get(String.class))) {

            stopTasks();
            TextFile.reloadAll();
            startTasks();
            sender.sendMessage(TextComponent.fromLegacyText(BungeeMessages.RELOADED.color()
                    .replace("%prefix%", BungeeMessages.PREFIX.color())));

            if (!(sender instanceof ProxiedPlayer)) {
                return;
            }

            final ProxiedPlayer player = (ProxiedPlayer) sender;

            if (player.getServer() == null) {
                return;
            }

            ByteArrayDataOutput buf = ByteStreams.newDataOutput();
            buf.writeUTF("RELOAD");

            player.getServer().sendData("cleanss:join", buf.toByteArray());

        } else {

            sender.sendMessage(TextComponent.fromLegacyText(BungeeMessages.NO_PERMISSION.color()
                    .replace("%prefix%", BungeeMessages.PREFIX.color())));

        }
    }

    private void startTasks() {
        List<ServerInfo> servers = Utils.getServerList(BungeeConfig.CONTROL.getStringList());
        List<ServerInfo> fallbacks = Utils.getServerList(BungeeConfig.CONTROL_FALLBACK.getStringList());

        for (ServerInfo server : servers) {
            Utils.startTask(server);
        }

        for (ServerInfo server : fallbacks) {
            Utils.startTask(server);
        }
    }

    private void stopTasks() {
        List<ServerInfo> servers = Utils.getServerList(BungeeConfig.CONTROL.getStringList());
        List<ServerInfo> fallbacks = Utils.getServerList(BungeeConfig.CONTROL_FALLBACK.getStringList());

        for (ServerInfo server : servers) {
            Utils.stopTask(server);
        }

        for (ServerInfo server : fallbacks) {
            Utils.stopTask(server);
        }
    }
}
