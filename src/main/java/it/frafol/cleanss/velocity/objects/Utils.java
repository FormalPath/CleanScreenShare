package it.frafol.cleanss.velocity.objects;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import it.frafol.cleanss.velocity.CleanSS;
import it.frafol.cleanss.velocity.enums.VelocityConfig;
import it.frafol.cleanss.velocity.enums.VelocityMessages;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@UtilityClass
public class Utils {

    private static final CleanSS instance = CleanSS.getInstance();

    public List<String> getStringList(@NotNull VelocityMessages velocityMessages) {
        return instance.getMessagesTextFile().getConfig().getStringList(velocityMessages.getPath());
    }

    @Getter
    private ScheduledTask titleTask;

    public List<String> getStringList(VelocityMessages velocityMessages, Placeholder... placeholders) {
        List<String> newList = new ArrayList<>();

        for (String s : getStringList(velocityMessages)) {
            s = applyPlaceHolder(s, placeholders);
            newList.add(s);
        }

        return newList;
    }

    public String applyPlaceHolder(String s, Placeholder @NotNull ... placeholders) {
        for (Placeholder placeholder : placeholders) {
            s = s.replace(placeholder.getKey(), placeholder.getValue());
        }

        return s;
    }

    public String color(@NotNull String s) {

        return s.replace("&", "§");

    }

    public List<String> color(@NotNull List<String> list) {
        return list.stream().map(Utils::color).collect(Collectors.toList());
    }

    public void sendList(CommandSource commandSource, @NotNull List<String> stringList, Player player_name) {

        for (String message : stringList) {

            if (message.contains(VelocityMessages.CONTROL_CLEAN_NAME.get(String.class))) {

                commandSource.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(message).clickEvent(ClickEvent
                        .clickEvent(ClickEvent.Action.SUGGEST_COMMAND, VelocityMessages.CONTROL_CLEAN_COMMAND.get(String.class)
                                .replace("%player%", player_name.getUsername()))));

            } else if (message.contains(VelocityMessages.CONTROL_CHEATER_NAME.get(String.class))) {

                commandSource.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(message).clickEvent(ClickEvent
                        .clickEvent(ClickEvent.Action.SUGGEST_COMMAND, VelocityMessages.CONTROL_CHEATER_COMMAND.get(String.class)
                                .replace("%player%", player_name.getUsername()))));

            } else if (message.contains(VelocityMessages.CONTROL_ADMIT_NAME.get(String.class))) {

                commandSource.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(message)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, VelocityMessages.CONTROL_ADMIT_COMMAND.get(String.class)
                                .replace("%player%", player_name.getUsername()))));

            } else if (message.contains(VelocityMessages.CONTROL_REFUSE_NAME.get(String.class))) {

                commandSource.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(message)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, VelocityMessages.CONTROL_REFUSE_COMMAND.get(String.class)
                                .replace("%player%", player_name.getUsername()))));

            } else {
                commandSource.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(message));
            }

        }
    }

    public void sendFormattedList(VelocityMessages velocityMessages, CommandSource commandSource, Player player_name, Placeholder... placeholders) {
        sendList(commandSource, color(getStringList(velocityMessages, placeholders)), player_name);
    }

    public void finishControl(@NotNull Player suspicious, @NotNull Player administrator, RegisteredServer proxyServer) {

        if (suspicious.isActive() && administrator.isActive()) {

            PlayerCache.getAdministrator().remove(administrator.getUniqueId());
            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());
            PlayerCache.getCouples().remove(administrator, suspicious);

            if (!suspicious.getCurrentServer().isPresent()) {
                return;
            }

            if (suspicious.getCurrentServer().get().getServer().getServerInfo().getName().equals(VelocityConfig.CONTROL.get(String.class))) {

                suspicious.createConnectionRequest(proxyServer).fireAndForget();

                Utils.sendEndTitle(suspicious);

                suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.FINISHSUS.color().replace("%prefix%", VelocityMessages.PREFIX.color())));

                if (!administrator.getCurrentServer().isPresent()) {
                    return;
                }

                if (administrator.getCurrentServer().get().getServer().getServerInfo().getName().equals(VelocityConfig.CONTROL.get(String.class))) {
                    administrator.createConnectionRequest(proxyServer).fireAndForget();
                }
            }

        } else if (suspicious.isActive()) {

            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());
            PlayerCache.getAdministrator().remove(administrator.getUniqueId());

            suspicious.createConnectionRequest(proxyServer).fireAndForget();

            Utils.sendEndTitle(suspicious);

            suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.FINISHSUS.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())));

            PlayerCache.getCouples().remove(administrator);

        } else if (administrator.isActive()) {

            PlayerCache.getAdministrator().remove(administrator.getUniqueId());
            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());

            administrator.createConnectionRequest(proxyServer).fireAndForget();

            administrator.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.LEAVESUS.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                    .replace("%player%", suspicious.getUsername())));

            PlayerCache.getCouples().remove(administrator);

        } else {

            PlayerCache.getAdministrator().remove(administrator.getUniqueId());
            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());
            PlayerCache.getCouples().remove(administrator);

        }
    }

    public void startControl(@NotNull Player suspicious, @NotNull Player administrator, RegisteredServer proxyServer) {

        PlayerCache.getAdministrator().add(administrator.getUniqueId());
        PlayerCache.getSuspicious().add(suspicious.getUniqueId());
        PlayerCache.getCouples().put(administrator, suspicious);

        if (!administrator.getCurrentServer().isPresent()) {
            return;
        }

        if (!suspicious.getCurrentServer().isPresent()) {
            return;
        }

        if (administrator.getCurrentServer().get().getServer() != proxyServer) {

            administrator.createConnectionRequest(proxyServer).fireAndForget();

        } else {

            Utils.sendChannelMessage(administrator, "ADMIN");

            if (administrator.getProtocolVersion().getProtocol() >= ProtocolVersion.getProtocolVersion(759).getProtocol()) {
                Utils.sendChannelMessage(administrator, "NO_CHAT");
            }
        }

        if (suspicious.getCurrentServer().get().getServer() != proxyServer) {

            suspicious.createConnectionRequest(proxyServer).fireAndForget();

        } else {

            Utils.sendChannelMessage(suspicious, "SUSPECT");

            if (suspicious.getProtocolVersion().getProtocol() >= ProtocolVersion.getProtocolVersion(759).getProtocol()) {
                Utils.sendChannelMessage(suspicious, "NO_CHAT");
            }
        }

        Utils.sendStartTitle(suspicious);
        Utils.checkForErrors(suspicious, administrator, proxyServer);

        suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.MAINSUS.color()
                .replace("%prefix%", VelocityMessages.PREFIX.color())));

        VelocityMessages.CONTROL_FORMAT.sendList(administrator, suspicious,
                new Placeholder("cleanname", VelocityMessages.CONTROL_CLEAN_NAME.color()),
                new Placeholder("hackername", VelocityMessages.CONTROL_CHEATER_NAME.color()),
                new Placeholder("admitname", VelocityMessages.CONTROL_ADMIT_NAME.color()),
                new Placeholder("refusename", VelocityMessages.CONTROL_REFUSE_NAME.color()));

    }

    @SuppressWarnings("UnstableApiUsage")
    public void sendChannelMessage(@NotNull Player player, String type) {

        final ByteArrayDataOutput buf = ByteStreams.newDataOutput();

        buf.writeUTF(type);
        buf.writeUTF(player.getUsername());
        player.getCurrentServer().ifPresent(sv ->
                sv.sendPluginMessage(CleanSS.channel_join, buf.toByteArray()));

    }

    private void checkForErrors(@NotNull Player suspicious, @NotNull Player administrator, RegisteredServer proxyServer) {

        instance.getServer().getScheduler().buildTask(instance, () -> {

            if (!(PlayerCache.getSuspicious().contains(suspicious.getUniqueId()) && PlayerCache.getAdministrator().contains(administrator.getUniqueId()))) {
                return;
            }

            if (!(suspicious.getCurrentServer().isPresent() || administrator.getCurrentServer().isPresent())) {
                return;
            }

            if (suspicious.getCurrentServer().get().getServer().equals(proxyServer) && administrator.getCurrentServer().get().getServer().equals(proxyServer)) {
                return;
            }

            final Optional<RegisteredServer> fallbackServer = instance.getServer().getServer(VelocityConfig.CONTROL_FALLBACK.get(String.class));

            if (!fallbackServer.isPresent()) {
                return;
            }

            Utils.finishControl(suspicious, administrator, fallbackServer.get());
            administrator.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NO_EXIST.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())));
            instance.getLogger().error("Your control server is not configured correctly or is crashed, please check the configuration file. " +
                    "The Control cannot be handled!");

        }).delay(2L, TimeUnit.SECONDS).schedule();
    }

    public boolean isConsole(CommandSource invocation) {
        return !(invocation instanceof Player);
    }

    private void sendStartTitle(Player suspicious) {

        if (VelocityMessages.CONTROL_USETITLE.get(Boolean.class)) {

            Title controlTitle = Title.title(

                    LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.CONTROL_TITLE.color()),
                    LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.CONTROL_SUBTITLE.color()),

                    Title.Times.times(
                            Duration.ofSeconds(VelocityMessages.CONTROL_FADEIN.get(Integer.class)),
                            Duration.ofSeconds(VelocityMessages.CONTROL_STAY.get(Integer.class)),
                            Duration.ofSeconds(VelocityMessages.CONTROL_FADEOUT.get(Integer.class))));

            titleTask = instance.getServer().getScheduler().buildTask(
                            instance, () -> suspicious.showTitle(controlTitle))
                    .delay(VelocityMessages.CONTROL_DELAY.get(Integer.class), TimeUnit.SECONDS)
                    .schedule();

        }
    }

    private void sendEndTitle(Player suspicious) {

        if (VelocityMessages.CONTROLFINISH_USETITLE.get(Boolean.class)) {

            Title controlTitle = Title.title(

                    LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.CONTROLFINISH_TITLE.color()),
                    LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.CONTROLFINISH_SUBTITLE.color()),

                    Title.Times.times(
                            Duration.ofSeconds(VelocityMessages.CONTROLFINISH_FADEIN.get(Integer.class)),
                            Duration.ofSeconds(VelocityMessages.CONTROLFINISH_STAY.get(Integer.class)),
                            Duration.ofSeconds(VelocityMessages.CONTROLFINISH_FADEOUT.get(Integer.class))));

            titleTask = instance.getServer().getScheduler().buildTask(
                            instance, () -> suspicious.showTitle(controlTitle))
                    .delay(VelocityMessages.CONTROLFINISH_DELAY.get(Integer.class), TimeUnit.SECONDS)
                    .schedule();

        }
    }
}