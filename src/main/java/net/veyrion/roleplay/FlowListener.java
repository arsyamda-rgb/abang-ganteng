package net.veyrion.roleplay;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import net.kyori.adventure.text.Component;

public final class FlowListener implements Listener {

    private final VeyrionPlugin pl;

    public FlowListener(VeyrionPlugin pl) {
        this.pl = pl;
    }

    private boolean locked(Player p) {
        return pl.frozen.contains(p.getUniqueId());
    }

    // ---------------------------------------------------------- join / quit

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (pl.pending(p)) {
            pl.freeze(p);
            Bukkit.getScheduler().runTaskLater(pl, () -> {
                if (p.isOnline() && locked(p)) pl.menus().showWelcome(p);
            }, 10L);
            // menu utama dibuka oleh watcher (maks 2 detik)
        } else {
            pl.applyPerks(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (locked(p)) pl.release(p); // jangan biarkan buta / invulnerable permanen
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(pl, () -> pl.applyPerks(p), 2L);
    }

    // ---------------------------------------------------------- pengunci

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!locked(e.getPlayer())) return;
        Location f = e.getFrom(), t = e.getTo();
        if (t == null) return;
        if (f.getX() != t.getX() || f.getY() != t.getY() || f.getZ() != t.getZ()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && locked(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    // ---------------------------------------------------------- klik menu

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menus.Holder) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menus.Holder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (pl.menus().rolling.contains(p.getUniqueId())) return;

        int slot = e.getRawSlot();
        Menus m = pl.menus();

        switch (holder.type) {
            case MAIN -> {
                if (slot == Menus.MAIN_START) {
                    p.playSound(p.getLocation(), "minecraft:ui.button.click", 1f, 1f);
                    m.openGacha(p);
                } else if (slot == Menus.MAIN_EXIT) {
                    p.kick(Component.text("Sampai jumpa di Veyrion Roleplay."));
                } else if (slot == Menus.MAIN_DISCORD) {
                    p.playSound(p.getLocation(), "minecraft:ui.button.click", 1f, 1f);
                    m.openDiscord(p);
                }
            }
            case GACHA -> {
                if (slot == Menus.GACHA_START) {
                    if (pl.data().get(p.getUniqueId()) == null) m.roll(p);
                } else if (slot == Menus.GACHA_BACK) {
                    m.openMain(p);
                }
            }
        }
    }
}
