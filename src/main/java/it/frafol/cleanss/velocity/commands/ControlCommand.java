package it.frafol.cleanss.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import it.frafol.cleanss.velocity.CleanSS;
import it.frafol.cleanss.velocity.enums.VelocityConfig;
import it.frafol.cleanss.velocity.enums.VelocityMessages;
import it.frafol.cleanss.velocity.handlers.LimboHandler;
import it.frafol.cleanss.velocity.objects.PlayerCache;
import it.frafol.cleanss.velocity.objects.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ControlCommand implements SimpleCommand {

	private final CleanSS instance;

	public ControlCommand(CleanSS instance) {
		this.instance = instance;
	}

	@Override
	public void execute(@NotNull Invocation invocation) {

		final CommandSource source = invocation.source();
		boolean luckperms = instance.getServer().getPluginManager().getPlugin("luckperms").isPresent();

		if (Utils.isConsole(source)) {
			source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.ONLY_PLAYERS.color()
					.replace("%prefix%", VelocityMessages.PREFIX.color())));
			return;
		}

		if (!source.hasPermission(VelocityConfig.CONTROL_PERMISSION.get(String.class))) {
			source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NO_PERMISSION.color()
					.replace("%prefix%", VelocityMessages.PREFIX.color())));
			return;
		}

		if (invocation.arguments().length == 0) {
			source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.USAGE.color().replace("%prefix%", VelocityMessages.PREFIX.color())));
			return;
		}

		if (invocation.arguments().length == 1) {

			if (instance.getServer().getAllPlayers().toString().contains(invocation.arguments()[0])) {

				final Optional<Player> player = instance.getServer().getPlayer(invocation.arguments()[0]);
				final Player sender = (Player) source;

				if (!player.isPresent()) {
					source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NOT_ONLINE.color()
							.replace("%prefix%", VelocityMessages.PREFIX.color())
							.replace("%player%", invocation.arguments()[0])));
					return;
				}

				if (player.get().hasPermission(VelocityConfig.BYPASS_PERMISSION.get(String.class))) {
					source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.PLAYER_BYPASS.color()
							.replace("%prefix%", VelocityMessages.PREFIX.color())));
					return;
				}


				if (instance.getServer().getAllServers().toString().contains(VelocityConfig.CONTROL.get(String.class)) || instance.useLimbo) {

					final Optional<RegisteredServer> proxyServer = instance.getServer().getServer(VelocityConfig.CONTROL.get(String.class));

					if (!proxyServer.isPresent() && !instance.useLimbo) {
						return;
					}

					if (sender.getUniqueId() == player.get().getUniqueId()) {
						source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.YOURSELF.color()
								.replace("%prefix%", VelocityMessages.PREFIX.color())));
						return;
					}

					if (PlayerCache.getSuspicious().contains(player.get().getUniqueId())) {
						source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.CONTROL_ALREADY.color()
								.replace("%prefix%", VelocityMessages.PREFIX.color())));
						return;
					}

					if (PlayerCache.getIn_control().get(player.get().getUniqueId()) != null && PlayerCache.getIn_control().get(player.get().getUniqueId()) == 1) {
						source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.CONTROL_ALREADY.color()
								.replace("%prefix%", VelocityMessages.PREFIX.color())));
						return;
					}

					String admin_group;
					String suspect_group;

					if (luckperms) {

						final LuckPerms api = LuckPermsProvider.get();

						final User admin = api.getUserManager().getUser(sender.getUniqueId());
						final User suspect = api.getUserManager().getUser(player.get().getUniqueId());

						if (admin == null || suspect == null) {
							return;
						}

						final String admingroup = admin.getCachedData().getMetaData().getPrimaryGroup();
						admin_group = admingroup == null ? "" : admingroup;

						final String suspectgroup = suspect.getCachedData().getMetaData().getPrimaryGroup();
						suspect_group = suspectgroup == null ? "" : suspectgroup;

					} else {
						admin_group = "";
						suspect_group = "";
					}

					if (instance.useLimbo) {

						instance.getLimbo().spawnPlayer(sender, new LimboHandler(sender, instance));
						instance.getLimbo().spawnPlayer(player.get(), new LimboHandler(player.get(), instance));

						Utils.startControl(player.get(), sender, null);
						Utils.sendDiscordMessage(player.get(), sender, VelocityMessages.DISCORD_STARTED.get(String.class).replace("%suspectgroup%", suspect_group).replace("%admingroup%", admin_group));

						return;
					}

					if (!proxyServer.isPresent()) {
						return;
					}

					if (!VelocityConfig.DISABLE_PING.get(Boolean.class)) {
						proxyServer.get().ping().whenComplete((result, throwable) -> {

							if (throwable != null) {
								source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NO_EXIST.color()
										.replace("%prefix%", VelocityMessages.PREFIX.color())));
								return;
							}

							if (result == null) {
								source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NO_EXIST.color()
										.replace("%prefix%", VelocityMessages.PREFIX.color())));
								return;
							}

							Utils.startControl(player.get(), sender, proxyServer.get());
							Utils.sendDiscordMessage(player.get(), sender, VelocityMessages.DISCORD_STARTED.get(String.class).replace("%suspectgroup%", suspect_group).replace("%admingroup%", admin_group));

						});
					} else {

						Utils.startControl(player.get(), sender, proxyServer.get());
						Utils.sendDiscordMessage(player.get(), sender, VelocityMessages.DISCORD_STARTED.get(String.class));

					}

				} else {

					source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NO_EXIST.color()
							.replace("%prefix%", VelocityMessages.PREFIX.color())));

				}

			} else {

				source.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.NOT_ONLINE.color()
						.replace("%prefix%", VelocityMessages.PREFIX.color())
						.replace("%player%", invocation.arguments()[0])));

			}
		}
	}

	@Override
	public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
		final String[] strings = invocation.arguments();

		if (Utils.isConsole(invocation.source())) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		final List<String> players = instance.getServer().getAllPlayers().stream()
				.map(Player::getUsername)
				.filter(player -> strings.length != 1 || strings[0].isEmpty()
						|| player.toLowerCase().startsWith(strings[0].toLowerCase()))
				.collect(Collectors.toList());

		return CompletableFuture.completedFuture(players);
	}
}