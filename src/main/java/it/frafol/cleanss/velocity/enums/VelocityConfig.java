package it.frafol.cleanss.velocity.enums;

import it.frafol.cleanss.velocity.CleanSS;
import org.jetbrains.annotations.NotNull;

public enum VelocityConfig {

    CONTROL_PERMISSION("permissions.control"),
    BYPASS_PERMISSION("permissions.bypass"),
    RELOAD_PERMISSION("permissions.reload"),

    CHECK_FOR_PROBLEMS("settings.check_for_problems"),

    UPDATE_CHECK("settings.update_check"),

    CONTROL("settings.server_name"),
    CONTROL_FALLBACK("settings.fallback_server_name"),

    STATS("settings.stats");

    private final String path;
    public static final CleanSS instance = CleanSS.getInstance();

    VelocityConfig(String path) {
        this.path = path;
    }

    public <T> T get(@NotNull Class<T> clazz) {
        return clazz.cast(instance.getConfigTextFile().getConfig().get(path));
    }

}