package it.frafol.cleanss.bukkit.listeners;

import it.frafol.cleanss.bukkit.CleanSS;
import it.frafol.cleanss.bukkit.enums.SpigotCache;
import it.frafol.cleanss.bukkit.enums.SpigotConfig;
import it.frafol.cleanss.bukkit.objects.PlayerCache;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PlayerListener implements Listener {

    private final CleanSS instance = CleanSS.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(@NotNull AsyncPlayerChatEvent event) {
        if (PlayerCache.getNo_chat().contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }

        if (SpigotConfig.CHAT.get(Boolean.class)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(@NotNull PlayerCommandPreprocessEvent event) {

        final Player player = event.getPlayer();

        if (player.hasPermission(SpigotConfig.STAFF_PERMISSION.get(String.class))) {
            return;
        }

        if (event.getMessage().startsWith("/")) {
            if (PlayerCache.getNo_chat().contains(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPvP(EntityDamageByEntityEvent event) {
        if (SpigotConfig.PVP.get(Boolean.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(@NotNull EntityDamageEvent event) {

        final Entity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();

        if (SpigotConfig.INVINCIBLE.get(Boolean.class)) {
            event.setCancelled(true);
        }

        if (event.getCause().equals(EntityDamageEvent.DamageCause.VOID) && SpigotConfig.VOID.get(Boolean.class)) {
            event.setCancelled(true);
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHunger(@NotNull FoodLevelChangeEvent event) {

        final HumanEntity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();

        if (SpigotConfig.HUNGER.get(Boolean.class)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void changeGameMode(@NotNull PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        switch (SpigotConfig.GAMEMODE.get(String.class)) {
            case "none":
                break;
            case "adventure":
                player.setGameMode(GameMode.ADVENTURE);
                break;
            case "survival":
                player.setGameMode(GameMode.SURVIVAL);
                break;
            case "creative":
                player.setGameMode(GameMode.CREATIVE);
                break;
            case "spectator":
                player.setGameMode(GameMode.SPECTATOR);
                break;
            default:
                instance.getLogger().severe(SpigotConfig.GAMEMODE.get(String.class) + " is an invalid gamemode!");
        }

        if (SpigotConfig.HUNGER.get(Boolean.class)) {
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void joinMessage(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        instance.getServer().getScheduler().runTaskLater(instance, () -> {
            if (PlayerCache.getSuspicious().contains(player.getUniqueId()) || PlayerCache.getAdministrator().contains(player.getUniqueId())) {
                return;
            }

            player.teleport(PlayerCache.StringToLocation(SpigotCache.OTHER_SPAWN.get(String.class)));
        }, 20L);

        if (Objects.equals(SpigotConfig.CUSTOM_JOIN_MESSAGE.get(String.class), "none")) {
            return;
        }

        if (Objects.equals(SpigotConfig.CUSTOM_JOIN_MESSAGE.get(String.class), "disabled")) {
            event.setJoinMessage(null);
            return;
        }

        event.setJoinMessage(SpigotConfig.CUSTOM_JOIN_MESSAGE.color().replace("%player%", event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void quitMessage(PlayerQuitEvent event) {

        if (Objects.equals(SpigotConfig.CUSTOM_LEAVE_MESSAGE.get(String.class), "none")) {
            return;
        }

        if (Objects.equals(SpigotConfig.CUSTOM_LEAVE_MESSAGE.get(String.class), "disabled")) {
            event.setQuitMessage(null);
            return;
        }

        event.setQuitMessage(SpigotConfig.CUSTOM_LEAVE_MESSAGE.color().replace("%player%", event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(@NotNull PlayerQuitEvent event) {

        Player player = event.getPlayer();

        PlayerCache.getNo_chat().remove(player.getUniqueId());

        if (PlayerCache.getSuspicious().contains(player.getUniqueId())) {
            PlayerCache.deleteSuspectScoreboard(player);
        }

        if (PlayerCache.getAdministrator().contains(player.getUniqueId())) {
            PlayerCache.deleteAdminScoreboard(player);
        }

        PlayerCache.getSuspicious().remove(player.getUniqueId());
        PlayerCache.getAdministrator().remove(player.getUniqueId());

        instance.stopTimer(player.getUniqueId());

        if (PlayerCache.getCouples().get(player.getUniqueId()) != null) {
            PlayerCache.getCouples().remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {

        final Player player = event.getPlayer();

        if (player.hasPermission(SpigotConfig.STAFF_PERMISSION.get(String.class))) {
            return;
        }

        if (SpigotConfig.MOVE.get(Boolean.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerBreak(BlockBreakEvent event) {

        final Player player = event.getPlayer();

        if (player.hasPermission(SpigotConfig.ADMIN_PERMISSION.get(String.class))) {
            return;
        }

        if (SpigotConfig.BREAK.get(Boolean.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPlace(BlockPlaceEvent event) {

        final Player player = event.getPlayer();

        if (player.hasPermission(SpigotConfig.ADMIN_PERMISSION.get(String.class))) {
            return;
        }

        if (SpigotConfig.PLACE.get(Boolean.class)) {
            event.setCancelled(true);
        }
    }
}
