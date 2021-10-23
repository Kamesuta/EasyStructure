package net.teamfruit.easystructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class I18n {
    private static Locale locale;

    public static String format(final String langKey, final Object... args) {
        if (locale == null)
            return langKey;
        return locale.format(langKey, args);
    }

    public static void setLocale(final String langDef) {
        I18n.locale = createLocale(langDef);
    }

    private static Locale createLocale(final String langDef) {
        Properties properties = new Properties();
        final File langDir = new File(EasyStructure.INSTANCE.getDataFolder(), "lang");
        final File pluginFile = EasyStructure.INSTANCE.getFile();
        try (ZipFile pluginZip = new ZipFile(pluginFile)) {
            final File resDef = new File(langDir, langDef);
            final ZipEntry entryDef = pluginZip.getEntry("lang/" + langDef);
            if (resDef.exists())
                try {
                    properties.load(new InputStreamReader(new FileInputStream(resDef), StandardCharsets.UTF_8));
                } catch (final IOException e) {
                }
            if (entryDef != null)
                try {
                    properties.load(new InputStreamReader(pluginZip.getInputStream(entryDef), StandardCharsets.UTF_8));
                } catch (final IOException e) {
                }
        } catch (final IOException e1) {
        }
        if (properties.isEmpty()) {
            Log.log.log(Level.WARNING, "Missing locale file for " + langDef);
            return null;
        }
        return new Locale(properties);
    }

    private static class Locale {
        private final Properties properties;

        private Locale(Properties properties) {
            this.properties = properties;
        }

        public String translate(final String p_135026_1_) {
            final String s1 = this.properties.getProperty(p_135026_1_);
            return s1 == null ? p_135026_1_ : s1;
        }

        public String format(final String langKey, final Object... args) {
            final String s1 = translate(langKey);

            try {
                return String.format(s1, args);
            } catch (final IllegalFormatException illegalformatexception) {
                return "Format error: " + s1;
            }
        }
    }
}
