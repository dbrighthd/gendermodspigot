package dbrighthd.wildfiregendermodplugin.commands;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacket;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DiagnosticCommand implements CommandExecutor, TabCompleter {
    private final GenderModPlugin plugin;

    public DiagnosticCommand(GenderModPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("wildfire.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0)
            return false;

        String sub = args[0].toLowerCase();
        if (sub.equals("status")) {
            Player target = args.length > 1 ? Bukkit.getPlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            int version = plugin.getUserManager().getProtocolVersion(target.getUniqueId());
            ModSyncPacket format = plugin.getNetworkManager().getPacketFormatForPlayer(target.getUniqueId());
            ModUser user = plugin.getUserManager().getUsers().get(target.getUniqueId());

            sender.sendMessage(ChatColor.AQUA + "--- Female Gender Mod Status: " + target.getName() + " ---");
            sender.sendMessage(ChatColor.YELLOW + "Protocol Version: " + ChatColor.WHITE +
                    (version == -1 ? "Unknown (Using default)" : version));
            sender.sendMessage(ChatColor.YELLOW + "Active Format: " + ChatColor.WHITE + format.getVersion() +
                    " (" + format.getModRange() + ")");
            sender.sendMessage(
                    ChatColor.YELLOW + "Sync Data: " + ChatColor.WHITE + (user != null ? "Present" : "None"));
            if (user != null) {
                sender.sendMessage(
                        ChatColor.GRAY + " - Gender: " + user.configuration().generalOptions().genderIdentity());
                sender.sendMessage(ChatColor.GRAY + " - Bust Size: " + user.configuration().breastOptions().bustSize());
            }

            return true;

        } else if (sub.equals("testsend")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command must be run by a player.");
                return true;
            }

            UUID dummyId = UUID.nameUUIDFromBytes("wildfire-dummy".getBytes());
            plugin.getNetworkManager().sendTestData(player, dummyId);
            sender.sendMessage(ChatColor.GREEN + "Sent dummy sync packet for UUID " + dummyId + " to you.");
            sender.sendMessage(ChatColor.GRAY + "Check client logs and look for processing logs for this UUID.");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "testsend").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            return null; // Player list
        }
        return new ArrayList<>();
    }
}
