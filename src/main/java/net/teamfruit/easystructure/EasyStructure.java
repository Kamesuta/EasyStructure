package net.teamfruit.easystructure;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class EasyStructure extends JavaPlugin {
    public File schematicDirectory;

    @Override
    public void onEnable() {
        // Plugin startup logic
        new Config(this).configure();

        schematicDirectory = new File(getDataFolder(), "schematics");
        schematicDirectory.mkdirs();

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getCommand("es").setExecutor(new CommandListener(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
