package net.veyrion.roleplay;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/** Jembatan ke ItemsAdder pakai reflection, jadi plugin tetap jalan tanpa ItemsAdder. */
public final class IA {
    private IA() {
    }

    public static boolean present() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    /** Ganti :nama_gambar: jadi karakter font image ItemsAdder. */
    public static String fonts(String text) {
        if (!present() || text == null) return text;
        try {
            Class<?> c = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            return (String) c.getMethod("replaceFontImages", String.class).invoke(null, text);
        } catch (Throwable t) {
            return text;
        }
    }

    /** Ambil item custom ItemsAdder (namespace:id). Null kalau tidak ada. */
    public static ItemStack item(String namespacedId) {
        if (!present() || namespacedId == null || namespacedId.isBlank()) return null;
        try {
            Class<?> c = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object stack = c.getMethod("getInstance", String.class).invoke(null, namespacedId);
            if (stack == null) return null;
            return (ItemStack) c.getMethod("getItemStack").invoke(stack);
        } catch (Throwable t) {
            return null;
        }
    }
}
