package it.frafol.cleanss.bungee;

import it.frafol.cleanss.bungee.commands.*;
import it.frafol.cleanss.bungee.enums.BungeeConfig;
import it.frafol.cleanss.bungee.enums.BungeeMessages;
import it.frafol.cleanss.bungee.enums.BungeeVersion;
import it.frafol.cleanss.bungee.listeners.ChatListener;
import it.frafol.cleanss.bungee.listeners.CommandListener;
import it.frafol.cleanss.bungee.listeners.KickListener;
import it.frafol.cleanss.bungee.listeners.ServerListener;
import it.frafol.cleanss.bungee.mysql.MySQLWorker;
import it.frafol.cleanss.bungee.objects.PlayerCache;
import it.frafol.cleanss.bungee.objects.TextFile;
import it.frafol.cleanss.bungee.objects.Utils;
import lombok.Getter;
import lombok.SneakyThrows;
import net.byteflux.libby.BungeeLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.YamlFile;
import ru.vyarus.yaml.updater.YamlUpdater;
import ru.vyarus.yaml.updater.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CleanSS extends Plugin {

    private TextFile messagesTextFile;
	private TextFile configTextFile;
	private TextFile versionTextFile;

	@Getter
	private JDA jda;

	@Getter
	private MySQLWorker data;

	@Getter
	private static CleanSS instance;

	public boolean updated = false;

	@Override
	public void onEnable() {

		instance = this;

		loadLibraries();

		getLogger().info("\n§d   ___  __    ____    __    _  _    ___  ___\n" +
				"  / __)(  )  ( ___)  /__\\  ( \\( )  / __)/ __)\n" +
				" ( (__  )(__  )__)  /(__)\\  )  (   \\__ \\\\__ \\\n" +
				"  \\___)(____)(____)(__)(__)(_)\\_)  (___/(___/\n");

		getLogger().info("§7Server version: §d" + getServerBrand() + " - " + getServerVersion());

		getLogger().info("§7Loading §dconfiguration§7...");
		loadFiles();
		updateConfig();

		getLogger().info("§7Loading §dplugin§7...");

		getProxy().registerChannel("cleanss:join");
		registerCommands();
		registerListeners();

		if (BungeeConfig.MYSQL.get(Boolean.class)) {
			data = new MySQLWorker();
			ControlTask();
		}

		if (BungeeConfig.DISCORD_ENABLED.get(Boolean.class)) {
			loadDiscord();
		}

		if (BungeeConfig.STATS.get(Boolean.class) && !getDescription().getVersion().contains("alpha")) {
			new Metrics(this, 17063);
			getLogger().info("§7Metrics loaded §dsuccessfully§7!");
		}

		UpdateChecker();
		startTasks();
		getLogger().info("§7Plugin §dsuccessfully §7loaded!");
	}

	public YamlFile getConfigTextFile() {
		return getInstance().configTextFile.getConfig();
	}

	public YamlFile getVersionTextFile() {
		return getInstance().versionTextFile.getConfig();
	}

	public YamlFile getMessagesTextFile() {
		return getInstance().messagesTextFile.getConfig();
	}

	private void startTasks() {
		List<ServerInfo> servers = Utils.getServerList(BungeeConfig.CONTROL.getStringList());
		List<ServerInfo> fallbacks = Utils.getServerList(BungeeConfig.CONTROL_FALLBACK.getStringList());
		servers.forEach(Utils::startTask);
		fallbacks.forEach(Utils::startTask);
	}

	private void stopTasks() {
		List<ServerInfo> servers = Utils.getServerList(BungeeConfig.CONTROL.getStringList());
		List<ServerInfo> fallbacks = Utils.getServerList(BungeeConfig.CONTROL_FALLBACK.getStringList());
		servers.forEach(Utils::stopTask);
		fallbacks.forEach(Utils::stopTask);
	}

	private void registerCommands() {
		getProxy().getPluginManager().registerCommand(this, new DebugCommand(this));
		getProxy().getPluginManager().registerCommand(this, new ControlCommand(this));
		getProxy().getPluginManager().registerCommand(this, new FinishCommand(this));

		if (BungeeConfig.ENABLE_SPECTATING.get(Boolean.class)) {
			getProxy().getPluginManager().registerCommand(this, new SpectateCommand(this));
		}

		getProxy().getPluginManager().registerCommand(this, new InfoCommand(this));
		getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
	}

	private void loadFiles() {
		configTextFile = new TextFile(getDataFolder().toPath(), "config.yml");
		messagesTextFile = new TextFile(getDataFolder().toPath(), "messages.yml");
		versionTextFile = new TextFile(getDataFolder().toPath(), "version.yml");
	}

	private void registerListeners() {

		getProxy().getPluginManager().registerListener(this, new ServerListener());
		getProxy().getPluginManager().registerListener(this, new CommandListener());

		if (BungeeMessages.CONTROL_CHAT.get(Boolean.class)) {
			getProxy().getPluginManager().registerListener(this, new ChatListener(this));
		}

		getProxy().getPluginManager().registerListener(this, new KickListener(this));
	}

	private void UpdateChecker() {

		if (!BungeeConfig.UPDATE_CHECK.get(Boolean.class)) {
			return;
		}

		new UpdateCheck(this).getVersion(version -> {

			if (Integer.parseInt(getDescription().getVersion().replace(".", "")) < Integer.parseInt(version.replace(".", ""))) {

				if (BungeeConfig.AUTO_UPDATE.get(Boolean.class) && !updated) {
					autoUpdate();
					return;
				}

				if (!updated) {
					getLogger().warning("§eThere is a new update available, download it on SpigotMC!");
				}
			}

			if (Integer.parseInt(getDescription().getVersion().replace(".", "")) > Integer.parseInt(version.replace(".", ""))) {
				getLogger().warning("§eYou are using a development version, please report any bugs!");
			}

		});
	}

	public boolean isWindows() {
		String os = System.getProperty("os.name");
		return os.startsWith("Windows");
	}

	public void autoUpdate() {

		if (isWindows()) {
			getLogger().warning("§eAuto update is not supported on Windows.");
			return;
		}

		new UpdateCheck(this).getVersion(version -> {
			String fileUrl = "https://github.com/frafol/CleanScreenShare/releases/download/release/cleanscreenshare-"+ version + " .jar";
			String destination = "./plugins/";

			String fileName = getFileNameFromUrl(fileUrl);
			File outputFile = new File(destination, fileName);

			try {
				downloadFile(fileUrl, outputFile);
			} catch (IOException ignored) {
				getLogger().warning("An error occurred while downloading the update, please download it manually from SpigotMC.");
			}

			updated = true;
			getLogger().warning("CleanScreenShare successfully updated, a restart is required.");
		});
	}

	private String getFileNameFromUrl(String fileUrl) {
		int slashIndex = fileUrl.lastIndexOf('/');
		if (slashIndex != -1 && slashIndex < fileUrl.length() - 1) {
			return fileUrl.substring(slashIndex + 1);
		}
		throw new IllegalArgumentException("Invalid file URL");
	}

	private void downloadFile(String fileUrl, File outputFile) throws IOException {
		URL url = new URL(fileUrl);
		try (InputStream inputStream = url.openStream()) {
			deleteFile(outputFile.getParent(), "cleanscreenshare-");
			Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@SneakyThrows
	public void deleteFile(String directoryPath, String file_start) {
		File directory = new File(directoryPath);

		if (!directory.isDirectory()) {
			throw new IllegalArgumentException();
		}

		File[] files = directory.listFiles();
		if (files == null) {
			throw new IOException();
		}

		for (File file : files) {
			if (file.isFile() && file.getName().startsWith(file_start)) {
				getLogger().warning("Found an old plugin file: " + file.getName());
				if (file.delete()) {
					getLogger().warning("Deleted old file: " + file.getName());
				}
			}
		}
	}

	public void ControlTask() {

		instance.getProxy().getScheduler().schedule(this, () -> {

			for (ProxiedPlayer players : getProxy().getPlayers()) {
				PlayerCache.getIn_control().put(players.getUniqueId(), data.getStats(players.getUniqueId(), "incontrol"));
				PlayerCache.getControls().put(players.getUniqueId(), data.getStats(players.getUniqueId(), "controls"));
				PlayerCache.getControls_suffered().put(players.getUniqueId(), data.getStats(players.getUniqueId(), "controls"));
			}

		}, 1L, 1L, TimeUnit.SECONDS);

	}

	public void UpdateChecker(ProxiedPlayer player) {

		if (!BungeeConfig.UPDATE_CHECK.get(Boolean.class)) {
			return;
		}

		new UpdateCheck(this).getVersion(version -> {

			if (Integer.parseInt(getDescription().getVersion().replace(".", "")) < Integer.parseInt(version.replace(".", ""))) {

				if (BungeeConfig.AUTO_UPDATE.get(Boolean.class) && !updated) {
					autoUpdate();
					return;
				}

				if (!updated) {
					player.sendMessage(TextComponent.fromLegacyText("§e[CleanScreenShare] There is a new update available, download it on SpigotMC!"));
				}
			}
		});
	}

	private void loadDiscord() {
		try {
			jda = JDABuilder.createDefault(BungeeConfig.DISCORD_TOKEN.get(String.class)).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
		} catch (ExceptionInInitializerError e) {
			getLogger().severe("Invalid Discord configuration, please check your config.yml file.");
			getLogger().severe("Make sure you are not using any strange forks (like Aegis).");
		}

		updateTaskJDA();
	}

	public void updateTaskJDA() {
		getProxy().getScheduler().schedule(this, this::updateJDA, 1L, 30L, TimeUnit.SECONDS);
	}

	public void updateJDA() {

		if (!BungeeConfig.DISCORD_ENABLED.get(Boolean.class)) {
			return;
		}

		if (jda == null) {
			getLogger().severe("Fatal error while updating JDA. Please report this error to https://discord.com/invite/sTSwaGBCdC.");
			return;
		}

		jda.getPresence().setActivity(Activity.of(Activity.ActivityType.valueOf
						(BungeeConfig.DISCORD_ACTIVITY_TYPE.get(String.class).toUpperCase()), BungeeConfig.DISCORD_ACTIVITY.get(String.class)
				.replace("%players%", String.valueOf(getProxy().getOnlineCount()))
				.replace("%suspiciouses%", String.valueOf(PlayerCache.getSuspicious().size()))));

	}

	@SneakyThrows
	private void updateConfig() {

		if (!BungeeVersion.VERSION.get(String.class).startsWith("2.0")) {
			getLogger().severe("§cThe configurations are really outdated and there may be duplicate values. To make sure you don't miss them, reset them!");
		}

		if (!getDescription().getVersion().equals(BungeeVersion.VERSION.get(String.class))) {

			getLogger().info("§7Creating new §dconfigurations§7...");
			YamlUpdater.create(new File(getDataFolder().toPath() + "/config.yml"), FileUtils.findFile("https://raw.githubusercontent.com/frafol/CleanScreenShare/main/src/main/resources/config.yml"))
					.backup(true)
					.update();
			YamlUpdater.create(new File(getDataFolder().toPath() + "/messages.yml"), FileUtils.findFile("https://raw.githubusercontent.com/frafol/CleanScreenShare/main/src/main/resources/messages.yml"))
					.backup(true)
					.update();
			versionTextFile.getConfig().set("version", getDescription().getVersion());
			versionTextFile.getConfig().save();
			loadFiles();

		}
	}

	private void loadLibraries() {

		BungeeLibraryManager bungeeLibraryManager = new BungeeLibraryManager(this);

		final Relocation yamlrelocation = new Relocation("yaml", "it{}frafol{}libs{}yaml");
		Library yaml = Library.builder()
				.groupId("me{}carleslc{}Simple-YAML")
				.artifactId("Simple-Yaml")
				.version("1.8.4")
				.relocate(yamlrelocation)
				.build();

		final Relocation updaterrelocation = new Relocation("updater", "it{}frafol{}libs{}updater");
		Library updater = Library.builder()
				.groupId("ru{}vyarus")
				.artifactId("yaml-config-updater")
				.version("1.4.2")
				.relocate(updaterrelocation)
				.build();

		final Relocation kotlin = new Relocation("kotlin", "it{}frafol{}libs{}kotlin");
		Library discord = Library.builder()
				.groupId("net{}dv8tion")
				.artifactId("JDA")
				.version("5.0.0-beta.13")
				.relocate(kotlin)
				.url("https://github.com/DV8FromTheWorld/JDA/releases/download/v5.0.0-beta.13/JDA-5.0.0-beta.13-withDependencies-min.jar")
				.build();

		bungeeLibraryManager.addMavenCentral();
		bungeeLibraryManager.addJitPack();

		try {
			bungeeLibraryManager.loadLibrary(yaml);
		} catch (RuntimeException ignored) {
			getLogger().severe("Failed to load Simple-YAML library. Trying to download it from GitHub...");
			yaml = Library.builder()
					.groupId("me{}carleslc{}Simple-YAML")
					.artifactId("Simple-Yaml")
					.version("1.8.4")
					.url("https://github.com/Carleslc/Simple-YAML/releases/download/1.8.4/Simple-Yaml-1.8.4.jar")
					.relocate(yamlrelocation)
					.build();
		}

		bungeeLibraryManager.loadLibrary(yaml);
		bungeeLibraryManager.loadLibrary(updater);
		bungeeLibraryManager.loadLibrary(discord);
	}

	@Override
	public void onDisable() {

		getProxy().unregisterChannel("cleanss:join");
		stopTasks();

		if (getConfigTextFile() == null || BungeeConfig.MYSQL.get(Boolean.class)) {

			getLogger().info("§7Closing §ddatabase§7...");
			for (ProxiedPlayer players : getProxy().getPlayers()) {
				if (data != null) {
					data.setInControl(players.getUniqueId(), 0);
					data.setControls(players.getUniqueId(), PlayerCache.getControls().get(players.getUniqueId()));
				}
			}

			if (data != null) {
				data.close();
			}
		}

		getLogger().info("§7Clearing §dinstances§7...");
		instance = null;

		getLogger().info("§7Plugin successfully §ddisabled§7!");
	}

	private String getServerBrand() {
		return getProxy().getName();
	}

	private String getServerVersion() {
		return getProxy().getVersion();
	}

	public <K, V> K getKey(@NotNull Map<K, V> map, V value) {

		for (Map.Entry<K, V> entry : map.entrySet()) {

			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}

		}
		return null;
	}

	public <K, V> V getValue(@NotNull Map<K, V> map, K key) {

		for (Map.Entry<K, V> entry : map.entrySet()) {

			if (entry.getKey().equals(key)) {
				return entry.getValue();
			}

		}
		return null;
	}

	public void setData() {
		data = new MySQLWorker();
	}

}