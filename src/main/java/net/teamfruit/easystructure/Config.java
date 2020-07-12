package net.teamfruit.easystructure;

import com.google.common.collect.Maps;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class Config {
    public static final String SETTING_PARTICLE_COLOR = "particle.color";
    public static final String SETTING_PARTICLE_RANGE = "particle.range";
    public static final String SETTING_PLACE_RANGE = "place.range";
    public static final String SETTING_PLACE_LOG = "place.log";

    public void configure() {
        // 設定を初期化
        FileConfiguration config = EasyStructure.INSTANCE.getConfig();
        final Map<String, Object> configInit = Maps.newHashMap();
        configInit.put(SETTING_PARTICLE_COLOR, 0xffffff);
        configInit.put(SETTING_PARTICLE_RANGE, 128);
        configInit.put(SETTING_PLACE_RANGE, 128);
        configInit.put(SETTING_PLACE_LOG, false);
        config.options().copyDefaults(true);
        config.addDefaults(configInit);
        EasyStructure.INSTANCE.saveConfig();
    }
}
