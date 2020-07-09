package net.teamfruit.easystructure;

import com.google.common.collect.Maps;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class Config {
    public static final String SETTING_PARTICLE_COLOR = "particle.color";
    public static final String SETTING_PARTICLE_RANGE = "particle.range";

    private final EasyStructure plugin;
    private final FileConfiguration config;

    public Config(EasyStructure plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void configure() {
        final Map<String, Object> configInit = Maps.newHashMap();
        configInit.put(SETTING_PARTICLE_COLOR, 0xffffff);
        configInit.put(SETTING_PARTICLE_RANGE, 32);
        config.options().copyDefaults(true);
        config.addDefaults(configInit);
        this.plugin.saveConfig();
    }
}
