package net.worldseed;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldSeedEntityEngine extends JavaPlugin {
    private static NamespacedKey KEY;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        KEY = new NamespacedKey(this, "WSEE");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public static NamespacedKey getNamespacedKey() {
        return KEY;
    }
}
