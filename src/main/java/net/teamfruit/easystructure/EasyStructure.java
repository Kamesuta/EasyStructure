package net.teamfruit.easystructure;

import org.bukkit.plugin.java.JavaPlugin;

public final class EasyStructure extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        new Config(this).configure();
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getCommand("es").setExecutor(new CommandListener(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
