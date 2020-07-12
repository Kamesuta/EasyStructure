package net.teamfruit.easystructure;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.history.changeset.ChangeSet;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ESSession {
    public List<UUID> entities = new ArrayList<>();
    public String currentSchematic;
    public Clipboard currentClipboard;
    public ChangeSet lastChangeSet;

    @Nullable
    public Clipboard getClipboardFromId(String uuid) {
        final Clipboard clipboard;
        if (uuid.equals(currentClipboard))
            clipboard = currentClipboard;
        else {
            // 保存先
            File file = new File(EasyStructure.INSTANCE.schematicDirectory, uuid + ".schem");

            if (!file.exists()) {
                return null;
            }

            // スケマティックをクリップボードに読み込み
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
            } catch (IOException e) {
                return null;
            }

            // スケマティックをセッションに保存
            currentSchematic = uuid;
            currentClipboard = clipboard;
        }
        return clipboard;
    }
}
