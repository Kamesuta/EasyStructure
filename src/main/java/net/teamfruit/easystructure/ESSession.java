package net.teamfruit.easystructure;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.changeset.BlockOptimizedHistory;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.teamfruit.easystructure.fakepaste.FakeExtent;
import net.teamfruit.easystructure.fakepaste.RealExtent;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ESSession {
    public String currentSchematic;
    public Clipboard currentClipboard;
    public ChangeSet lastChangeSet;
    public String lastUuid;
    public BlockVector3 lastPosition;
    public boolean lastVisible;

    @Nullable
    public Clipboard getClipboardCachedFromId(@Nullable String uuid) {
        if (uuid == null)
            return null;

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

    public void updateFakeSchematic(Player wPlayer, BlockVector3 wPosition, Clipboard clipboard, boolean isVisible) {
        // Fakeロールバック
        if (lastChangeSet != null) {
            Extent realExtent = new RealExtent(wPlayer.getWorld(), wPlayer);
            UndoContext context = new UndoContext();
            context.setExtent(realExtent);
            Operations.completeBlindly(ChangeSetExecutor.createUndo(lastChangeSet, context));
            lastChangeSet = null;
        }

        if (clipboard != null && wPosition != null && isVisible) {
            // フェイクブロック送信
            lastChangeSet = new BlockOptimizedHistory();
            Extent fakeExtent = new FakeExtent(wPlayer.getWorld(), wPlayer);
            Extent fakeChangeExtent = new ChangeSetExtent(fakeExtent, lastChangeSet);
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(fakeChangeExtent)
                    .to(wPosition)
                    .ignoreAirBlocks(true)
                    // configure here
                    .build();
            Operations.completeBlindly(operation);
        }
    }
}
