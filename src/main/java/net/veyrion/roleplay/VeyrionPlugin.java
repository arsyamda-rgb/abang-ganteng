package net.veyrion.roleplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VeyrionPlugin extends JavaPlugin {

    private DataStore data;
    private Menus menus;

    /** Player yang sedang dikunci (belum punya ras). */
    public final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    /** Waktu sampai kapan menu utama tidak dibuka paksa (mis. saat buku Discord terbuka). */
    public final Map<UUID, Long> grace = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataStore(this);
        menus = new Menus(this);

        getServer().getPluginManager().registerEvents(new FlowListener(this), this);
        VeyrionCommand cmd = new VeyrionCommand(this);
        getCommand("veyrion").setExecutor(cmd);
        getCommand("veyrion").setTabCompleter(cmd);

        // pemain yang sudah online saat /reload
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (pending(p)) freeze(p);
            else applyPerks(p);
        }

        // watcher: kalau pemain terkunci tapi tidak ada menu terbuka, buka lagi menu utama
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();
                if (!frozen.contains(id) || menus.rolling.contains(id)) continue;
                if (grace.getOrDefault(id, 0L) > now) continue;
                if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menus.Holder) continue;
                menus.openMain(p);
            }
        }, 40L, 10L);
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (frozen.contains(p.getUniqueId())) release(p);
        }
        if (data != null) data.save();
    }

    public DataStore data() {
        return data;
    }

    public Menus menus() {
        return menus;
    }

    public boolean pending(Player p) {
        return !p.hasPermission("veyrion.bypass") && data.get(p.getUniqueId()) == null;
    }

    public void freeze(Player p) {
        frozen.add(p.getUniqueId());
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION,
                0, false, false, false));
        p.setInvulnerable(true);
    }

    public void release(Player p) {
        frozen.remove(p.getUniqueId());
        grace.remove(p.getUniqueId());
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.setInvulnerable(false);
    }

    public void applyPerks(Player p) {
        Race r = Race.byId(data.get(p.getUniqueId()));
        if (r == null) return;
        for (String s : getConfig().getStringList("races." + r.id + ".effects")) {
            try {
                String[] a = s.split(":");
                PotionEffectType type = PotionEffectType.getByName(a[0].trim().toUpperCase());
                if (type == null) continue;
                int amp = a.length > 1 ? Integer.parseInt(a[1].trim()) : 0;
                p.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, amp, true, false, false));
            } catch (Exception ignored) {
            }
        }
    }
}
