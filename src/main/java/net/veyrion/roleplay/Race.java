package net.veyrion.roleplay;

import org.bukkit.Material;

public enum Race {
    ELF("elf", "Elf", Material.BOW, 1101, "PANAHMU",
            "minecraft:block.amethyst_block.chime", "Anggun, lincah, dan akrab dengan alam."),
    BEAST("beast", "Beast", Material.BONE, 1102, "CAKARMU",
            "minecraft:entity.wolf.howl", "Naluri liar dan taring yang tak kenal takut."),
    HUMAN("human", "Human", Material.IRON_SWORD, 1103, "TEKADMU",
            "minecraft:entity.player.levelup", "Lemah di awal, tak terbatas di akhir."),
    DRAGON("dragon", "Dragon", Material.DRAGON_EGG, 1104, "SAYAPMU",
            "minecraft:entity.ender_dragon.growl", "Darah naga, api, dan langit.");

    public final String id;
    public final String display;
    public final Material material;
    public final int modelData;
    public final String show;      // kata untuk "AYO TUNJUKKAN ___ DI DUNIA VEYRION"
    public final String sound;
    public final String desc;

    Race(String id, String display, Material material, int modelData, String show, String sound, String desc) {
        this.id = id;
        this.display = display;
        this.material = material;
        this.modelData = modelData;
        this.show = show;
        this.sound = sound;
        this.desc = desc;
    }

    public static Race byId(String id) {
        if (id == null) return null;
        for (Race r : values()) if (r.id.equalsIgnoreCase(id)) return r;
        return null;
    }
}
