package net.veyrion.roleplay;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Menus {

    public enum Type { MAIN, GACHA }

    public static final class Holder implements InventoryHolder {
        public final Type type;
        private Inventory inv;

        Holder(Type type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    // Slot layout
    public static final int MAIN_LOGO = 4, MAIN_START = 22, MAIN_EXIT = 31, MAIN_DISCORD = 40;
    public static final int GACHA_START = 22, GACHA_BACK = 18, GACHA_POINTER = 4;

    static final TextColor WHITE = NamedTextColor.WHITE;
    static final TextColor GOLD = TextColor.color(0xD4AF37);
    static final TextColor GRAY = TextColor.color(0x9A9A9A);
    static final TextColor DARKGOLD = TextColor.color(0x8A6D1A);

    private final VeyrionPlugin pl;
    private final Random rnd = new Random();
    public final Set<UUID> rolling = ConcurrentHashMap.newKeySet();

    public Menus(VeyrionPlugin pl) {
        this.pl = pl;
    }

    // ------------------------------------------------------------------ teks

    public Component txt(String text, TextColor color, boolean bold) {
        Component c = Component.text(text, color)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, bold);
        if (pl.getConfig().getBoolean("font.enabled", true)) {
            c = c.font(Key.key(pl.getConfig().getString("font.key", "veyrion:fantasy")));
        }
        return c;
    }

    private Component brandTitle() {
        return txt("Veyrion ", WHITE, true).append(txt("Roleplay", GOLD, true));
    }

    private Component guiTitle(String path) {
        String raw = pl.getConfig().getString(path, "");
        if (IA.present() && raw != null && !raw.isBlank()) {
            return LegacyComponentSerializer.legacySection().deserialize(IA.fonts(raw));
        }
        return brandTitle();
    }

    // ------------------------------------------------------------------ item

    private ItemStack base(String cfgPath, Material fallback, int cmd) {
        ItemStack it = IA.item(pl.getConfig().getString(cfgPath, ""));
        if (it == null) {
            it = new ItemStack(fallback);
            if (cmd > 0) {
                ItemMeta m = it.getItemMeta();
                m.setCustomModelData(cmd);
                it.setItemMeta(m);
            }
        }
        return it.clone();
    }

    private ItemStack named(ItemStack it, Component name, List<Component> lore, boolean glow) {
        ItemMeta m = it.getItemMeta();
        m.displayName(name);
        if (lore != null) m.lore(lore);
        if (glow) m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack pane(Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(" "));
        it.setItemMeta(m);
        return it;
    }

    private void frame(Inventory inv) {
        if (!pl.getConfig().getBoolean("gui.fill-glass", true)) return;
        int rows = inv.getSize() / 9;
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9, col = i % 9;
            boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
            inv.setItem(i, pane(edge ? (i % 2 == 0 ? Material.WHITE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                    : Material.BLACK_STAINED_GLASS_PANE));
        }
    }

    private ItemStack raceIcon(Race r, boolean center) {
        ItemStack it = base("races." + r.id + ".ia-item", r.material,
                pl.getConfig().getInt("races." + r.id + ".model-data", r.modelData));
        List<Component> lore = new ArrayList<>();
        lore.add(txt(r.desc, GRAY, false));
        return named(it, txt(r.display, center ? GOLD : WHITE, true), lore, center);
    }

    // ------------------------------------------------------------------ menu utama

    public void openMain(Player p) {
        Holder h = new Holder(Type.MAIN);
        Inventory inv = Bukkit.createInventory(h, 54, guiTitle("gui.main-title"));
        h.inv = inv;
        frame(inv);

        inv.setItem(MAIN_LOGO, named(base("gui.logo-item", Material.NETHER_STAR, 1001),
                txt("Veyrion", WHITE, true),
                List.of(txt("Roleplay", GOLD, true)), true));

        inv.setItem(MAIN_START, named(base("gui.start-item", Material.ENDER_EYE, 1002),
                txt("Tentukan Ras", WHITE, true),
                List.of(txt("Serahkan dirimu pada takdir.", GRAY, false),
                        Component.empty(),
                        txt("Klik untuk memulai", GOLD, false)), false));

        inv.setItem(MAIN_EXIT, named(base("gui.exit-item", Material.BARRIER, 1003),
                txt("Keluar", WHITE, true),
                List.of(txt("Tinggalkan Veyrion untuk sementara.", GRAY, false)), false));

        inv.setItem(MAIN_DISCORD, named(base("gui.discord-item", Material.PAPER, 1004),
                txt("Discord", WHITE, true),
                List.of(txt("Gabung komunitas Veyrion.", GRAY, false),
                        Component.empty(),
                        txt("Klik untuk membuka link", GOLD, false)), false));
        p.openInventory(inv);
    }

    public void openDiscord(Player p) {
        String url = pl.getConfig().getString("discord.url", "");
        Component link = Component.text("[ Klik di sini untuk masuk Discord ]", DARKGOLD)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.openUrl(url));
        Component page = Component.text("Veyrion\n", NamedTextColor.BLACK)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("Roleplay\n\n", DARKGOLD).decoration(TextDecoration.BOLD, true))
                .append(Component.text("Gabung bersama para petualang lainnya.\n\n", NamedTextColor.BLACK)
                        .decoration(TextDecoration.BOLD, false))
                .append(link);
        // beri waktu supaya menu utama tidak langsung menimpa buku
        pl.grace.put(p.getUniqueId(), System.currentTimeMillis() + 25_000L);
        p.openBook(Book.book(Component.text("Veyrion"), Component.text("Veyrion"), page));
        p.sendMessage(Component.text("Discord Veyrion: ", WHITE).append(
                Component.text(url, GOLD).clickEvent(ClickEvent.openUrl(url))));
    }

    // ------------------------------------------------------------------ menu gacha

    public void openGacha(Player p) {
        Holder h = new Holder(Type.GACHA);
        Inventory inv = Bukkit.createInventory(h, 27, guiTitle("gui.gacha-title"));
        h.inv = inv;
        frame(inv);

        Race[] all = Race.values();
        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < all.length; i++) inv.setItem(slots[i], raceIcon(all[i], false));

        inv.setItem(GACHA_POINTER, named(pane(Material.WHITE_STAINED_GLASS_PANE),
                txt("v", GOLD, true), null, false));
        inv.setItem(GACHA_START, named(base("gui.start-item", Material.ENDER_EYE, 1002),
                txt("Putar Takdir", WHITE, true),
                List.of(txt("Ras tidak bisa diulang.", GRAY, false),
                        Component.empty(),
                        txt("Klik untuk gacha", GOLD, false)), true));
        inv.setItem(GACHA_BACK, named(new ItemStack(Material.ARROW),
                txt("Kembali", WHITE, true), null, false));
        p.openInventory(inv);
    }

    private Race pick() {
        Race[] all = Race.values();
        int total = 0;
        int[] w = new int[all.length];
        for (int i = 0; i < all.length; i++) {
            w[i] = Math.max(0, pl.getConfig().getInt("races." + all[i].id + ".weight", 25));
            total += w[i];
        }
        if (total <= 0) return all[rnd.nextInt(all.length)];
        int x = rnd.nextInt(total);
        for (int i = 0; i < all.length; i++) {
            x -= w[i];
            if (x < 0) return all[i];
        }
        return all[0];
    }

    public void roll(Player p) {
        UUID id = p.getUniqueId();
        if (pl.data().get(id) != null || !rolling.add(id)) return;

        Race result = pick();
        int steps = 30 + rnd.nextInt(8);
        Race[] all = Race.values();
        List<Race> seq = new ArrayList<>();
        for (int i = 0; i < steps + 7; i++) seq.add(all[rnd.nextInt(all.length)]);
        seq.set(steps + 3, result);

        Inventory inv = p.getOpenInventory().getTopInventory();
        // bersihkan baris reel & tombol
        for (int i = 9; i < 18; i++) inv.setItem(i, pl.getConfig().getBoolean("gui.fill-glass", true)
                ? pane(Material.BLACK_STAINED_GLASS_PANE) : null);
        inv.setItem(GACHA_START, pl.getConfig().getBoolean("gui.fill-glass", true)
                ? pane(Material.BLACK_STAINED_GLASS_PANE) : null);
        inv.setItem(GACHA_BACK, pl.getConfig().getBoolean("gui.fill-glass", true)
                ? pane(Material.WHITE_STAINED_GLASS_PANE) : null);
        step(p, inv, seq, 0, steps, result);
    }

    private void step(Player p, Inventory inv, List<Race> seq, int s, int steps, Race result) {
        if (!p.isOnline()) {
            rolling.remove(p.getUniqueId());
            return;
        }
        for (int i = 0; i < 7; i++) inv.setItem(10 + i, raceIcon(seq.get(s + i), i == 3));
        p.playSound(p.getLocation(), "minecraft:block.note_block.chime", 0.7f, 0.7f + Math.min(1.2f, s * 0.03f));

        if (s >= steps) {
            Bukkit.getScheduler().runTaskLater(pl, () -> finish(p, result), 30L);
            return;
        }
        long delay = 2 + (long) (s * s) / (steps * 2L);
        Bukkit.getScheduler().runTaskLater(pl, () -> step(p, inv, seq, s + 1, steps, result), delay);
    }

    // ------------------------------------------------------------------ hasil

    private void finish(Player p, Race r) {
        rolling.remove(p.getUniqueId());
        if (!p.isOnline()) {
            // tetap simpan hasil supaya tidak bisa reroll lewat relog
            pl.data().set(p.getUniqueId(), r.id);
            return;
        }
        pl.data().set(p.getUniqueId(), r.id);
        pl.release(p);
        p.closeInventory();
        pl.applyPerks(p);

        Title.Times t1 = Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3400), Duration.ofMillis(400));
        p.showTitle(Title.title(
                txt(r.display.toUpperCase(), GOLD, true),
                txt("SELAMAT KAMU MENJADI RAS", WHITE, false), t1));

        Bukkit.getScheduler().runTaskLater(pl, () -> {
            if (!p.isOnline()) return;
            Title.Times t2 = Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(4000), Duration.ofMillis(800));
            p.showTitle(Title.title(
                    txt("AYO TUNJUKKAN", WHITE, true),
                    txt(r.show + " DI DUNIA VEYRION", GOLD, false), t2));
        }, 90L);

        p.sendMessage(Component.empty());
        p.sendMessage(txt("SELAMAT KAMU MENJADI RAS " + r.display.toUpperCase(), GOLD, true));
        p.sendMessage(txt("AYO TUNJUKKAN " + r.show + " DI DUNIA VEYRION", WHITE, false));
        p.sendMessage(Component.empty());

        p.playSound(p.getLocation(), "minecraft:ui.toast.challenge_complete", 1f, 1f);
        p.playSound(p.getLocation(), r.sound, 1f, 1f);
        launchFirework(p.getLocation());
    }

    private void launchFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc.clone().add(0, 1, 0), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.WHITE, Color.fromRGB(0xD4AF37))
                .withFade(Color.BLACK)
                .withFlicker().withTrail().build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(pl, fw::detonate, 2L);
    }

    // ------------------------------------------------------------------ title join

    public void showWelcome(Player p) {
        p.showTitle(Title.title(
                txt("Veyrion", WHITE, true),
                txt("Roleplay", GOLD, true),
                Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(3000), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), "minecraft:block.beacon.activate", 0.6f, 1.4f);
    }
}
