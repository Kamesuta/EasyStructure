package net.teamfruit.easystructure;

import com.google.common.collect.Maps;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class Config {
    public static final String SETTING_PLACE_RANGE = "place.range";
    public static final String SETTING_PLACE_LOG = "place.log";
    public static final String SETTING_PLACE_MESSAGE = "place.message";
    public static final String SETTING_PLACE_CUSTOM_ITEM = "place.customItem";
    public static final String SETTING_PLACE_PREVIEW = "place.preview";
    public static final String SETTING_LOCALE = "locale.file";

    public void configure() {
        // 設定を初期化
        FileConfiguration config = EasyStructure.INSTANCE.getConfig();
        final Map<String, Object> configInit = Maps.newHashMap();
        configInit.put(SETTING_PLACE_RANGE, 128);
        configInit.put(SETTING_PLACE_LOG, false);
        configInit.put(SETTING_PLACE_MESSAGE, true);
        configInit.put(SETTING_PLACE_CUSTOM_ITEM, false);
        configInit.put(SETTING_PLACE_PREVIEW, true);
        configInit.put(SETTING_LOCALE, "en_US.lang");
        config.options().copyDefaults(true);
        config.addDefaults(configInit);
        EasyStructure.INSTANCE.saveConfig();
    }
}
