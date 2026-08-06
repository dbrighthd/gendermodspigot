package dbrighthd.wildfiregendermodplugin.logging;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;

public class CustomPluginLogger {
    private final GenderModPlugin plugin;

    private final boolean enabled;
    private final boolean debug;
    private final boolean verbose;

    public CustomPluginLogger(GenderModPlugin plugin) {
        this.plugin = plugin;

        FileConfiguration pluginConfig = plugin.getConfig();
        this.enabled = pluginConfig.getBoolean("logging.enabled", true);
        this.debug = pluginConfig.getBoolean("logging.debug", false);
        this.verbose = pluginConfig.getBoolean("logging.verbose", false);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String hexDump(byte[] data) {
        if (data == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public void debug(String message, Object... params) {
        if (debug)
            log(Level.INFO, message, params);
    }

    public void info(String message, Object... params) {
        log(Level.INFO, message, params);
    }

    public void warning(String message, Object... params) {
        log(Level.WARNING, message, params);
    }

    public void severe(String message, Object... params) {
        log(Level.SEVERE, message, params);
    }

    public void log(Level level, String message, Object... params) {
        // Always emit WARNING and SEVERE regardless of the enabled flag so that
        // critical failures are never silently swallowed.
        if (!enabled && level.intValue() < Level.WARNING.intValue())
            return;

        if (params.length == 0) {
            plugin.getLogger().log(level, message);
            return;
        }

        plugin.getLogger().log(level, () -> message.formatted(params));
    }

    public void debug(Throwable throwable, String message, Object... params) {
        if (debug)
            thrown(Level.INFO, throwable, message, params);
    }

    public void info(Throwable throwable, String message, Object... params) {
        thrown(Level.INFO, throwable, message, params);
    }

    public void warning(Throwable throwable, String message, Object... params) {
        thrown(Level.WARNING, throwable, message, params);
    }

    public void severe(Throwable throwable, String message, Object... params) {
        thrown(Level.SEVERE, throwable, message, params);
    }

    public void thrown(Level level, Throwable throwable, String message, Object... params) {
        if (!enabled && level.intValue() < Level.WARNING.intValue())
            return;

        if (params.length == 0) {
            plugin.getLogger().log(level, message, throwable);
            return;
        }

        plugin.getLogger().log(level, throwable, () -> message.formatted(params));
    }
}
