package net.teamfruit.easystructure;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class EasyStructure extends JavaPlugin {
    public static EasyStructure INSTANCE;

    public File schematicDirectory;
    public ESSessionManager sessionManager;

    @Override
    public void onEnable() {
        INSTANCE = this;

        // Plugin startup logic
        new Config().configure();

        // 言語
        String locale = getConfig().getString(Config.SETTING_LOCALE);
        I18n.setLocale(locale);

        // スケマティックフォルダ初期化
        schematicDirectory = new File(getDataFolder(), "schematics");
        schematicDirectory.mkdirs();

        // セッションマネージャー
        sessionManager = new ESSessionManager();

        // イベント登録
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        getCommand("es").setExecutor(new CommandListener());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public File getFile() {
        return super.getFile();
    }
}
