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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class Utils {

    private static final CleanSS instance = CleanSS.getInstance();

    public List<String> getStringList(@NotNull VelocityMessages velocityMessages) {
        return instance.getMessagesTextFile().getConfig().getStringList(velocityMessages.getPath());
    }

    @Getter
    private ScheduledTask titleTask;

    @Getter
    private ScheduledTask titleTaskAdmin;

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

    public String color(String string) {
        String hex = convertHexColors(string);
        return hex.replace("&", "§");
    }

    private String convertHexColors(String message) {

        if (!containsHexColor(message)) {
            return message;
        }

        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder();
            for (char c : ch) {
                builder.append("&").append(c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return message;
    }

    private boolean containsHexColor(String message) {
        String hexColorPattern = "(?i)&#[a-f0-9]{6}";
        return message.matches(".*" + hexColorPattern + ".*");
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

    public void sendDiscordMessage(Player suspect, Player staffer, String message) {

        if (VelocityConfig.DISCORD_ENABLED.get(Boolean.class)) {

            if (instance.getJda() == null) {
                return;
            }

            if (instance.getJda().getJda() == null) {
                return;
            }

            final TextChannel channel = instance.getJda().getJda().getTextChannelById(VelocityConfig.DISCORD_CHANNEL_ID.get(String.class));

            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle(VelocityConfig.DISCORD_EMBED_TITLE.get(String.class), null);

            embed.setDescription(message
                    .replace("%suspect%", suspect.getUsername())
                    .replace("%staffer%", staffer.getUsername()));

            embed.setColor(Color.RED);
            embed.setFooter("Powered by CleanScreenShare");

            channel.sendMessageEmbeds(embed.build()).queue();

        }
    }

    public void sendDiscordMessage(Player suspect, Player staffer, String message, String result) {

        if (VelocityConfig.DISCORD_ENABLED.get(Boolean.class)) {

            final TextChannel channel = instance.getJda().getJda().getTextChannelById(VelocityConfig.DISCORD_CHANNEL_ID.get(String.class));

            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();

            embed.setTitle(VelocityConfig.DISCORD_EMBED_TITLE.get(String.class), null);

            embed.setDescription(message
                    .replace("%suspect%", suspect.getUsername())
                    .replace("%staffer%", staffer.getUsername())
                    .replace("%result%", result));

            embed.setColor(Color.RED);
            embed.setFooter("Powered by CleanScreenShare");

            channel.sendMessageEmbeds(embed.build()).queue();

        }
    }

    private String addCapital(String string) {
		if (string == null || string.isEmpty()) {
			return string;
		}

		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

    public void punishPlayer(UUID administrator, String suspicious, Player administrator_user, Player suspect) {

        boolean luckperms = instance.getServer().getPluginManager().isLoaded("luckperms");
        String admin_group = "";
        String suspect_group = "";

        if (luckperms) {

            final LuckPerms api = LuckPermsProvider.get();

            final User admin = api.getUserManager().getUser(administrator_user.getUniqueId());
            final User suspect2 = api.getUserManager().getUser(suspect.getUniqueId());

            if (admin == null || suspect2 == null) {
                return;
            }

            final Group admingroup = api.getGroupManager().getGroup(admin.getPrimaryGroup());

            String admingroup_displayname;
            if (admingroup != null) {
                admingroup_displayname = admingroup.getFriendlyName();

                if (admingroup_displayname.equalsIgnoreCase("default")) {
                    admingroup_displayname = VelocityMessages.DISCORD_LUCKPERMS_FIX.get(String.class);
                }

            } else {
                admingroup_displayname = "";
            }

            admin_group = admingroup == null ? "" : admingroup_displayname;

            final Group suspectgroup = api.getGroupManager().getGroup(suspect2.getPrimaryGroup());

            String suspectroup_displayname;
            if (suspectgroup != null) {
                suspectroup_displayname = suspectgroup.getFriendlyName();

                if (suspectroup_displayname.equalsIgnoreCase("default")) {
                    suspectroup_displayname = VelocityMessages.DISCORD_LUCKPERMS_FIX.get(String.class);
                }

            } else {
                suspectroup_displayname = "";
            }

            suspect_group = suspectgroup == null ? "" : suspectroup_displayname;

        }

        if (PlayerCache.getBan_execution().contains(administrator)) {

            if (VelocityMessages.DISCORD_CAPITAL.get(Boolean.class)) {
                Utils.sendDiscordMessage(suspect, administrator_user, VelocityMessages.DISCORD_FINISHED.get(String.class).replace("%suspectgroup%", addCapital(suspect_group)).replace("%admingroup%", addCapital(admin_group)), VelocityMessages.CHEATER.get(String.class));
            } else {
                Utils.sendDiscordMessage(suspect, administrator_user, VelocityMessages.DISCORD_FINISHED.get(String.class).replace("%suspectgroup%", suspect_group).replace("%admingroup%", admin_group), VelocityMessages.CHEATER.get(String.class));
            }

            String admin_prefix;
            String admin_suffix;
            String sus_prefix;
            String sus_suffix;

            if (luckperms) {

                final LuckPerms api = LuckPermsProvider.get();

                final User admin = api.getUserManager().getUser(administrator_user.getUniqueId());
                final User suspect1 = api.getUserManager().getUser(suspect.getUniqueId());

                if (admin == null) {
                    return;
                }

                if (suspect1 == null) {
                    return;
                }

                final String prefix1 = admin.getCachedData().getMetaData().getPrefix();
                final String suffix1 = admin.getCachedData().getMetaData().getSuffix();

                final String prefix2 = suspect1.getCachedData().getMetaData().getPrefix();
                final String suffix2 = suspect1.getCachedData().getMetaData().getSuffix();

                admin_prefix = prefix1 == null ? "" : prefix1;
                admin_suffix = suffix1 == null ? "" : suffix1;

                sus_prefix = prefix2 == null ? "" : prefix2;
                sus_suffix = suffix2 == null ? "" : suffix2;

            } else {
                admin_prefix = "";
                admin_suffix = "";
                sus_prefix = "";
                sus_suffix = "";
            }

            if (VelocityConfig.SEND_ADMIN_MESSAGE.get(Boolean.class)) {
                instance.getServer().getAllPlayers().stream()
                        .filter(players -> players.hasPermission(VelocityConfig.CONTROL_PERMISSION.get(String.class)))
                        .forEach(players -> players.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMIN_NOTIFY.color()
                                .replace("%prefix%", VelocityMessages.PREFIX.color())
                                .replace("%admin%", administrator_user.getUsername())
                                .replace("%suspect%", suspect.getUsername())
                                .replace("%adminprefix%", Utils.color(admin_prefix))
                                .replace("%adminsuffix%", Utils.color(admin_suffix))
                                .replace("%suspectprefix%", Utils.color(sus_prefix))
                                .replace("%suspectsuffix%", Utils.color(sus_suffix))
                                .replace("%result%", VelocityMessages.CHEATER.color()))));
            }
            return;
        }

        if (VelocityMessages.DISCORD_CAPITAL.get(Boolean.class)) {
            Utils.sendDiscordMessage(suspect, administrator_user, VelocityMessages.DISCORD_QUIT.get(String.class).replace("%suspectgroup%", addCapital(suspect_group)).replace("%admingroup%", addCapital(admin_group)), VelocityMessages.LEFT.get(String.class));
        } else {
            Utils.sendDiscordMessage(suspect, administrator_user, VelocityMessages.DISCORD_QUIT.get(String.class).replace("%suspectgroup%", suspect_group).replace("%admingroup%", admin_group), VelocityMessages.LEFT.get(String.class));
        }

        String admin_prefix;
        String admin_suffix;
        String sus_prefix;
        String sus_suffix;

        if (luckperms) {

            final LuckPerms api = LuckPermsProvider.get();

            final User admin = api.getUserManager().getUser(administrator_user.getUniqueId());
            final User suspect1 = api.getUserManager().getUser(suspect.getUniqueId());

            if (admin == null) {
                return;
            }

            if (suspect1 == null) {
                return;
            }

            final String prefix1 = admin.getCachedData().getMetaData().getPrefix();
            final String suffix1 = admin.getCachedData().getMetaData().getSuffix();

            final String prefix2 = suspect1.getCachedData().getMetaData().getPrefix();
            final String suffix2 = suspect1.getCachedData().getMetaData().getSuffix();

            admin_prefix = prefix1 == null ? "" : prefix1;
            admin_suffix = suffix1 == null ? "" : suffix1;

            sus_prefix = prefix2 == null ? "" : prefix2;
            sus_suffix = suffix2 == null ? "" : suffix2;

        } else {
            admin_prefix = "";
            admin_suffix = "";
            sus_prefix = "";
            sus_suffix = "";
        }

        if (VelocityConfig.SEND_ADMIN_MESSAGE.get(Boolean.class)) {
            instance.getServer().getAllPlayers().stream()
                    .filter(players -> players.hasPermission(VelocityConfig.CONTROL_PERMISSION.get(String.class)))
                    .forEach(players -> players.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMIN_NOTIFY.color()
                            .replace("%prefix%", VelocityMessages.PREFIX.color())
                            .replace("%admin%", administrator_user.getUsername())
                            .replace("%suspect%", suspect.getUsername())
                            .replace("%adminprefix%", Utils.color(admin_prefix))
                            .replace("%adminsuffix%", Utils.color(admin_suffix))
                            .replace("%suspectprefix%", Utils.color(sus_prefix))
                            .replace("%suspectsuffix%", Utils.color(sus_suffix))
                            .replace("%result%", VelocityMessages.LEFT.color()))));
        }

        if (!VelocityConfig.SLOG_PUNISH.get(Boolean.class)) {
            return;
        }

        instance.getServer().getCommandManager().executeAsync(instance.getServer().getConsoleCommandSource(), VelocityConfig.SLOG_COMMAND.get(String.class).replace("%player%", suspicious));

    }

    public void sendFormattedList(VelocityMessages velocityMessages, CommandSource commandSource, Player player_name, Placeholder... placeholders) {
        sendList(commandSource, color(getStringList(velocityMessages, placeholders)), player_name);
    }

    public void finishControl(@NotNull Player suspicious, @NotNull Player administrator, RegisteredServer proxyServer) {

        if (suspicious.isActive() && administrator.isActive()) {

            PlayerCache.getAdministrator().remove(administrator.getUniqueId());
            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());
            PlayerCache.getCouples().remove(administrator, suspicious);

            if (VelocityConfig.MYSQL.get(Boolean.class)) {
                instance.getData().setInControl(suspicious.getUniqueId(), 0);
                instance.getData().setInControl(administrator.getUniqueId(), 0);
            } else {
                PlayerCache.getIn_control().put(suspicious.getUniqueId(), 0);
                PlayerCache.getIn_control().put(administrator.getUniqueId(), 0);
            }

            if (!suspicious.getCurrentServer().isPresent()) {
                if (!instance.useLimbo) {
                    return;
                }
            }

            if (instance.useLimbo || suspicious.getCurrentServer().get().getServer().getServerInfo().getName().equals(VelocityConfig.CONTROL.get(String.class))) {

                if (!VelocityConfig.USE_DISCONNECT.get(Boolean.class) || instance.useLimbo) {

                    if (instance.useLimbo) {
                        LimboUtils.disconnect(suspicious, proxyServer);
                    } else {
                        suspicious.createConnectionRequest(proxyServer).fireAndForget();
                    }


                } else {
                    Utils.sendChannelMessage(suspicious, "DISCONNECT_NOW");
                }

                Utils.sendEndTitle(suspicious);
                Utils.sendAdminEndTitle(administrator, suspicious);

                suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.FINISHSUS.color()
                        .replace("%prefix%", VelocityMessages.PREFIX.color())));

                if (!administrator.getCurrentServer().isPresent()) {
                    if (!instance.useLimbo) {
                        return;
                    }
                }

                if (instance.useLimbo || administrator.getCurrentServer().get().getServer().getServerInfo().getName().equals(VelocityConfig.CONTROL.get(String.class))) {
                    if (!VelocityConfig.USE_DISCONNECT.get(Boolean.class) || instance.useLimbo) {

                        if (instance.useLimbo) {
                            LimboUtils.disconnect(administrator, proxyServer);
                        } else {
                            administrator.createConnectionRequest(proxyServer).fireAndForget();
                        }

                    } else {
                        Utils.sendChannelMessage(administrator, "DISCONNECT_NOW");
                    }
                }
            }

        } else if (suspicious.isActive()) {

            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());
            PlayerCache.getAdministrator().remove(administrator.getUniqueId());

            if (VelocityConfig.MYSQL.get(Boolean.class)) {
                instance.getData().setInControl(suspicious.getUniqueId(), 0);
                instance.getData().setInControl(administrator.getUniqueId(), 0);
            } else {
                PlayerCache.getIn_control().put(suspicious.getUniqueId(), 0);
                PlayerCache.getIn_control().put(administrator.getUniqueId(), 0);
            }

            if (!VelocityConfig.USE_DISCONNECT.get(Boolean.class) || instance.useLimbo) {

                if (instance.useLimbo) {
                    LimboUtils.disconnect(suspicious, proxyServer);

                } else {
                    suspicious.createConnectionRequest(proxyServer).fireAndForget();
                }

            } else {
                Utils.sendChannelMessage(suspicious, "DISCONNECT_NOW");
            }

            Utils.sendEndTitle(suspicious);
            Utils.sendAdminEndTitle(administrator, suspicious);

            suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.FINISHSUS.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())));

            PlayerCache.getCouples().remove(administrator);

        } else if (administrator.isActive()) {

            PlayerCache.getAdministrator().remove(administrator.getUniqueId());
            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());

            if (VelocityConfig.MYSQL.get(Boolean.class)) {
                instance.getData().setInControl(suspicious.getUniqueId(), 0);
                instance.getData().setInControl(administrator.getUniqueId(), 0);
            } else {
                PlayerCache.getIn_control().put(suspicious.getUniqueId(), 0);
                PlayerCache.getIn_control().put(administrator.getUniqueId(), 0);
            }

            if (!VelocityConfig.USE_DISCONNECT.get(Boolean.class) || instance.useLimbo) {

                if (instance.useLimbo) {
                    LimboUtils.disconnect(administrator, proxyServer);

                } else {
                    administrator.createConnectionRequest(proxyServer).fireAndForget();
                }

            } else {
                Utils.sendChannelMessage(administrator, "DISCONNECT_NOW");
            }

            administrator.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.LEAVESUS.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                    .replace("%player%", suspicious.getUsername())));

            PlayerCache.getCouples().remove(administrator);

        } else {

            PlayerCache.getAdministrator().remove(administrator.getUniqueId());
            PlayerCache.getSuspicious().remove(suspicious.getUniqueId());
            PlayerCache.getCouples().remove(administrator);

            if (VelocityConfig.MYSQL.get(Boolean.class)) {
                instance.getData().setInControl(suspicious.getUniqueId(), 0);
                instance.getData().setInControl(administrator.getUniqueId(), 0);
            } else {
                PlayerCache.getIn_control().put(suspicious.getUniqueId(), 0);
                PlayerCache.getIn_control().put(administrator.getUniqueId(), 0);
            }
        }
    }

    public void startControl(@NotNull Player suspicious, @NotNull Player administrator, RegisteredServer proxyServer) {

        String admin_prefix;
        String admin_suffix;
        String sus_prefix;
        String sus_suffix;

        boolean luckperms = instance.getServer().getPluginManager().getPlugin("luckperms").isPresent();
        if (luckperms) {

            final LuckPerms api = LuckPermsProvider.get();

            final User admin = api.getUserManager().getUser(administrator.getUniqueId());
            final User suspect = api.getUserManager().getUser(suspicious.getUniqueId());

            if (admin == null) {
                return;
            }

            if (suspect == null) {
                return;
            }

            final String prefix1 = admin.getCachedData().getMetaData().getPrefix();
            final String suffix1 = admin.getCachedData().getMetaData().getSuffix();

            final String prefix2 = suspect.getCachedData().getMetaData().getPrefix();
            final String suffix2 = suspect.getCachedData().getMetaData().getSuffix();

            admin_prefix = prefix1 == null ? "" : prefix1;
            admin_suffix = suffix1 == null ? "" : suffix1;

            sus_prefix = prefix2 == null ? "" : prefix2;
            sus_suffix = suffix2 == null ? "" : suffix2;

        } else {
            admin_prefix = "";
            admin_suffix = "";
            sus_prefix = "";
            sus_suffix = "";
        }

        if (instance.useLimbo) {

            if (VelocityConfig.CHECK_FOR_PROBLEMS.get(Boolean.class)) {
                PlayerCache.getNow_started_sus().add(suspicious.getUniqueId());
            }

            PlayerCache.getAdministrator().add(administrator.getUniqueId());
            PlayerCache.getSuspicious().add(suspicious.getUniqueId());
            PlayerCache.getCouples().put(administrator, suspicious);

            if (VelocityConfig.MYSQL.get(Boolean.class)) {

                instance.getData().setInControl(suspicious.getUniqueId(), 1);
                instance.getData().setInControl(administrator.getUniqueId(), 1);

                if (instance.getData().getStats(administrator.getUniqueId(), "controls") != -1) {
                    instance.getData().setControls(administrator.getUniqueId(), instance.getData().getStats(administrator.getUniqueId(), "controls") + 1);
                }

                if (instance.getData().getStats(suspicious.getUniqueId(), "suffered") != -1) {
                    instance.getData().setControlsSuffered(suspicious.getUniqueId(), instance.getData().getStats(suspicious.getUniqueId(), "suffered") + 1);
                }

            } else {

                PlayerCache.getIn_control().put(suspicious.getUniqueId(), 1);
                PlayerCache.getIn_control().put(administrator.getUniqueId(), 1);

                if (PlayerCache.getControls().get(administrator.getUniqueId()) != null) {
                    PlayerCache.getControls().put(administrator.getUniqueId(), PlayerCache.getControls().get(administrator.getUniqueId()) + 1);
                } else {
                    PlayerCache.getControls().put(administrator.getUniqueId(), 1);
                }

                if (PlayerCache.getControls_suffered().get(suspicious.getUniqueId()) != null) {
                    PlayerCache.getControls_suffered().put(suspicious.getUniqueId(), PlayerCache.getControls_suffered().get(suspicious.getUniqueId()) + 1);
                } else {
                    PlayerCache.getControls_suffered().put(suspicious.getUniqueId(), 1);
                }

            }

            Utils.sendStartTitle(suspicious);
            Utils.sendAdminStartTitle(administrator, suspicious);

            if (VelocityConfig.CHECK_FOR_PROBLEMS.get(Boolean.class)) {
                Utils.checkForErrors(suspicious, administrator, proxyServer);
            }

            if (VelocityConfig.SEND_ADMIN_MESSAGE.get(Boolean.class)) {
                instance.getServer().getAllPlayers().stream()
                        .filter(players -> players.hasPermission(VelocityConfig.CONTROL_PERMISSION.get(String.class)))
                        .forEach(players -> players.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMIN_NOTIFY.color()
                                .replace("%prefix%", VelocityMessages.PREFIX.color())
                                .replace("%admin%", administrator.getUsername())
                                .replace("%suspect%", suspicious.getUsername())
                                .replace("%adminprefix%", color(admin_prefix))
                                .replace("%adminsuffix%", color(admin_suffix))
                                .replace("%suspectprefix%", color(sus_prefix))
                                .replace("%suspectsuffix%", color(sus_suffix)))));
            }

            suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.MAINSUS.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                    .replace("%administrator%", administrator.getUsername())
                    .replace("%suspect%", suspicious.getUsername())
                    .replace("%adminprefix%", admin_prefix)
                    .replace("%adminsuffix%", admin_suffix)
                    .replace("%suspectprefix%", sus_prefix)
                    .replace("%suspectsuffix%", sus_suffix)));

            VelocityMessages.CONTROL_FORMAT.sendList(administrator, suspicious,
                    new Placeholder("cleanname", VelocityMessages.CONTROL_CLEAN_NAME.color()),
                    new Placeholder("hackername", VelocityMessages.CONTROL_CHEATER_NAME.color()),
                    new Placeholder("admitname", VelocityMessages.CONTROL_ADMIT_NAME.color()),
                    new Placeholder("refusename", VelocityMessages.CONTROL_REFUSE_NAME.color()),
                    new Placeholder("prefix", VelocityMessages.PREFIX.color()),
                    new Placeholder("suspect", suspicious.getUsername()),
                    new Placeholder("administrator", administrator.getUsername()),
                    new Placeholder("adminprefix", admin_prefix),
                    new Placeholder("adminsuffix", admin_suffix),
                    new Placeholder("suspectprefix", sus_prefix),
                    new Placeholder("suspectsuffix", sus_suffix));

            return;
        }

        if (!administrator.getCurrentServer().isPresent()) {
            return;
        }

        if (!suspicious.getCurrentServer().isPresent()) {
            return;
        }

        if (administrator.getCurrentServer().get().getServer() != proxyServer) {

            administrator.createConnectionRequest(proxyServer).fireAndForget();

        } else {

            Utils.sendChannelAdvancedMessage(administrator, suspicious, "ADMIN");

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

        PlayerCache.getAdministrator().add(administrator.getUniqueId());
        PlayerCache.getSuspicious().add(suspicious.getUniqueId());
        PlayerCache.getCouples().put(administrator, suspicious);

        if (VelocityConfig.MYSQL.get(Boolean.class)) {

            instance.getData().setInControl(suspicious.getUniqueId(), 1);
            instance.getData().setInControl(administrator.getUniqueId(), 1);

            if (instance.getData().getStats(administrator.getUniqueId(), "controls") != -1) {
                instance.getData().setControls(administrator.getUniqueId(), instance.getData().getStats(administrator.getUniqueId(), "controls") + 1);
            }

            if (instance.getData().getStats(suspicious.getUniqueId(), "suffered") != -1) {
                instance.getData().setControlsSuffered(suspicious.getUniqueId(), instance.getData().getStats(suspicious.getUniqueId(), "suffered") + 1);
            }

        } else {

            PlayerCache.getIn_control().put(suspicious.getUniqueId(), 1);
            PlayerCache.getIn_control().put(administrator.getUniqueId(), 1);

            if (PlayerCache.getControls().get(administrator.getUniqueId()) != null) {
                PlayerCache.getControls().put(administrator.getUniqueId(), PlayerCache.getControls().get(administrator.getUniqueId()) + 1);
            } else {
                PlayerCache.getControls().put(administrator.getUniqueId(), 1);
            }

            if (PlayerCache.getControls_suffered().get(suspicious.getUniqueId()) != null) {
                PlayerCache.getControls_suffered().put(suspicious.getUniqueId(), PlayerCache.getControls_suffered().get(suspicious.getUniqueId()) + 1);
            } else {
                PlayerCache.getControls_suffered().put(suspicious.getUniqueId(), 1);
            }

        }

        Utils.sendStartTitle(suspicious);
        Utils.sendAdminStartTitle(administrator, suspicious);

        if (VelocityConfig.CHECK_FOR_PROBLEMS.get(Boolean.class)) {
            Utils.checkForErrors(suspicious, administrator, proxyServer);
        }

        if (VelocityConfig.SEND_ADMIN_MESSAGE.get(Boolean.class)) {
            instance.getServer().getAllPlayers().stream()
                    .filter(players -> players.hasPermission(VelocityConfig.CONTROL_PERMISSION.get(String.class)))
                    .forEach(players -> players.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMIN_NOTIFY.color()
                            .replace("%prefix%", VelocityMessages.PREFIX.color())
                            .replace("%admin%", administrator.getUsername())
                            .replace("%suspect%", suspicious.getUsername())
                            .replace("%adminprefix%", color(admin_prefix))
                            .replace("%adminsuffix%", color(admin_suffix))
                            .replace("%suspectprefix%", color(sus_prefix))
                            .replace("%suspectsuffix%", color(sus_suffix)))));
        }

        suspicious.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.MAINSUS.color()
                .replace("%prefix%", VelocityMessages.PREFIX.color())
                .replace("%administrator%", administrator.getUsername())
                .replace("%suspect%", suspicious.getUsername())
                .replace("%adminprefix%", color(admin_prefix))
                .replace("%adminsuffix%", color(admin_suffix))
                .replace("%suspectprefix%", color(sus_prefix))
                .replace("%suspectsuffix%", color(sus_suffix))));

        VelocityMessages.CONTROL_FORMAT.sendList(administrator, suspicious,
                new Placeholder("cleanname", VelocityMessages.CONTROL_CLEAN_NAME.color()),
                new Placeholder("hackername", VelocityMessages.CONTROL_CHEATER_NAME.color()),
                new Placeholder("admitname", VelocityMessages.CONTROL_ADMIT_NAME.color()),
                new Placeholder("refusename", VelocityMessages.CONTROL_REFUSE_NAME.color()),
                new Placeholder("prefix", VelocityMessages.PREFIX.color()),
                new Placeholder("suspect", suspicious.getUsername()),
                new Placeholder("administrator", administrator.getUsername()),
                new Placeholder("adminprefix", color(admin_prefix)),
                new Placeholder("adminsuffix", color(admin_suffix)),
                new Placeholder("suspectprefix", color(sus_prefix)),
                new Placeholder("suspectsuffix", color(sus_suffix)));

    }

    @SuppressWarnings("UnstableApiUsage")
    public void sendChannelMessage(@NotNull Player player, String type) {

        final ByteArrayDataOutput buf = ByteStreams.newDataOutput();

        buf.writeUTF(type);
        buf.writeUTF(player.getUsername());
        player.getCurrentServer().ifPresent(sv ->
                sv.sendPluginMessage(CleanSS.channel_join, buf.toByteArray()));

    }

    @SuppressWarnings("UnstableApiUsage")
    public void sendChannelAdvancedMessage(@NotNull Player administrator, Player suspicious, String type) {

        final ByteArrayDataOutput buf = ByteStreams.newDataOutput();

        buf.writeUTF(type);
        buf.writeUTF(administrator.getUsername());
        buf.writeUTF(suspicious.getUsername());
        administrator.getCurrentServer().ifPresent(sv ->
                sv.sendPluginMessage(CleanSS.channel_join, buf.toByteArray()));

    }

    private void checkForErrors(@NotNull Player suspicious, @NotNull Player administrator, RegisteredServer proxyServer) {

        instance.getServer().getScheduler().buildTask(instance, () -> {

            if (instance.useLimbo) {
                PlayerCache.getNow_started_sus().remove(suspicious.getUniqueId());
                return;
            }

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
                suspicious.disconnect(LegacyComponentSerializer.legacy('§').deserialize("Your control server is not configured correctly or is crashed, please check the configuration file. " +
                        "The Control cannot be handled!"));
                administrator.disconnect(LegacyComponentSerializer.legacy('§').deserialize("Your control server is not configured correctly or is crashed, please check the configuration file. " +
                        "The Control cannot be handled!"));
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

        if (!VelocityMessages.CONTROL_USETITLE.get(Boolean.class)) {
            return;
        }

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

    private void sendAdminStartTitle(Player administrator, Player suspicious) {

        if (!VelocityMessages.ADMINCONTROL_USETITLE.get(Boolean.class)) {
            return;
        }

        Title controlTitle = Title.title(

                LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMINCONTROL_TITLE.color().replace("%suspect%", suspicious.getUsername())),
                LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMINCONTROL_SUBTITLE.color().replace("%suspect%", suspicious.getUsername())),

                Title.Times.times(
                        Duration.ofSeconds(VelocityMessages.ADMINCONTROL_FADEIN.get(Integer.class)),
                        Duration.ofSeconds(VelocityMessages.ADMINCONTROL_STAY.get(Integer.class)),
                        Duration.ofSeconds(VelocityMessages.ADMINCONTROL_FADEOUT.get(Integer.class))));

        titleTaskAdmin = instance.getServer().getScheduler().buildTask(
                        instance, () -> administrator.showTitle(controlTitle))
                .delay(VelocityMessages.ADMINCONTROL_DELAY.get(Integer.class), TimeUnit.SECONDS)
                .schedule();
    }

    private void sendEndTitle(Player suspicious) {

        if (!VelocityMessages.CONTROLFINISH_USETITLE.get(Boolean.class)) {
            return;
        }

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

    private void sendAdminEndTitle(Player administrator, Player suspicious) {

        if (!VelocityMessages.ADMINCONTROLFINISH_USETITLE.get(Boolean.class)) {
            return;
        }

        Title controlTitle = Title.title(

                LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMINCONTROLFINISH_TITLE.color().replace("%player%", suspicious.getUsername())),
                LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ADMINCONTROLFINISH_SUBTITLE.color().replace("%player%", suspicious.getUsername())),

                Title.Times.times(
                        Duration.ofSeconds(VelocityMessages.ADMINCONTROLFINISH_FADEIN.get(Integer.class)),
                        Duration.ofSeconds(VelocityMessages.ADMINCONTROLFINISH_STAY.get(Integer.class)),
                        Duration.ofSeconds(VelocityMessages.ADMINCONTROLFINISH_FADEOUT.get(Integer.class))));

        titleTaskAdmin = instance.getServer().getScheduler().buildTask(
                        instance, () -> administrator.showTitle(controlTitle))
                .delay(VelocityMessages.ADMINCONTROLFINISH_DELAY.get(Integer.class), TimeUnit.SECONDS)
                .schedule();
    }
}