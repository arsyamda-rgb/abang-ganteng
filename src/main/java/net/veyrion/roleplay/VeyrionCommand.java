package net.veyrion.roleplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class VeyrionCommand implements CommandExecutor, TabCompleter {

    private final VeyrionPlugin pl;

    public VeyrionCommand(VeyrionPlugin pl) {
        this.pl = pl;
    }

    private void msg(CommandSender s, String text) {
        s.sendMessage(Component.text("[Veyrion] ", NamedTextColor.GOLD).append(Component.text(text, NamedTextColor.WHITE)));
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            msg(s, "/veyrion <info|reload|reset|menu>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "info" -> {
                Player target = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
                if (target == null) {
                    msg(s, "Player tidak ditemukan.");
                    return true;
                }
                Race r = Race.byId(pl.data().get(target.getUniqueId()));
                msg(s, target.getName() + " : " + (r == null ? "belum punya ras" : r.display));
            }
            case "reload" -> {
                if (!s.hasPermission("veyrion.admin")) return deny(s);
                pl.reloadConfig();
                msg(s, "Config dimuat ulang.");
            }
            case "reset" -> {
                if (!s.hasPermission("veyrion.admin")) return deny(s);
                if (a.length < 2) {
                    msg(s, "/veyrion reset <player>");
                    return true;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    msg(s, "Player harus online.");
                    return true;
                }
                pl.data().remove(t.getUniqueId());
                t.getActivePotionEffects().forEach(e -> {
                    if (e.getDuration() > 1_000_000 || e.isInfinite()) t.removePotionEffect(e.getType());
                });
                if (pl.pending(t)) pl.freeze(t);
                msg(s, "Ras " + t.getName() + " direset, dia akan gacha ulang.");
            }
            case "menu" -> {
                if (!s.hasPermission("veyrion.admin")) return deny(s);
                if (s instanceof Player p) pl.menus().openMain(p);
            }
            default -> msg(s, "/veyrion <info|reload|reset|menu>");
        }
        return true;
    }

    private boolean deny(CommandSender s) {
        s.sendMessage(Component.text("Kamu tidak punya izin.", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String x : List.of("info", "reload", "reset", "menu")) {
                if (x.startsWith(a[0].toLowerCase())) out.add(x);
            }
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("reset") || a[0].equalsIgnoreCase("info"))) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        return out;
    }
}
