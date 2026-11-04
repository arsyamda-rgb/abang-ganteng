package net.veyrion.roleplay;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Menyimpan ras tiap player di data.yml */
public final class DataStore {
    private final File file;
    private final YamlConfiguration yaml;

    public DataStore(JavaPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public String get(UUID id) {
        return yaml.getString("players." + id);
    }

    public void set(UUID id, String race) {
        yaml.set("players." + id, race);
        save();
    }

    public void remove(UUID id) {
        yaml.set("players." + id, null);
        save();
    }

    public synchronized void save() {
        try {
            yaml.save(file);
        } catch (IOException ignored) {
        }
    }
}
