package net.teamfruit.easystructure;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
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
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import net.teamfruit.easystructure.fakepaste.DummyExtent;
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
    public boolean lastVisible;
    public Timer timer = new Timer();
    public int yawOffsetInt;
    public String lastUuid;
    public BlockVector3 lastPosition;
    public int lastYawInt;
    public int lastYawOffsetInt;

    public static class Timer {
        private long lastTime;

        public Timer() {
            reset();
        }

        public void reset() {
            lastTime = System.currentTimeMillis();
        }

        public long getTime() {
            return System.currentTimeMillis() - lastTime;
        }
    }

    public boolean isValidId(String uuid) {
        if (uuid.equals(currentClipboard))
            return true;

        // 保存先
        File file = new File(EasyStructure.INSTANCE.schematicDirectory, uuid + ".schem");

        if (!file.exists())
            return false;

        return true;
    }

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

    public static class PasteOperation {
        private BlockVector3 wPosition;
        private Clipboard clipboard;
        private Extent destination;
        private int yawInt;

        public PasteOperation(BlockVector3 wPosition, Clipboard clipboard, Extent destination, int yawInt) {
            this.wPosition = wPosition;
            this.clipboard = clipboard;
            this.destination = destination;
            this.yawInt = yawInt;
        }
    }

    private Operation createPlaceOperation(BlockVector3 wPosition, Clipboard clipboard, Extent destination, int yawInt) {
        ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
        Transform transform = new AffineTransform().rotateY(yawInt * -90.0);
        clipboardHolder.setTransform(transform);

        BlockVector3 posSize0 = clipboard.getDimensions();
        BlockVector3 posSize1 = BlockVector3.at(posSize0.getBlockX() / 2, 0, posSize0.getBlockZ());
        BlockVector3 posSize2 = transform.apply(posSize1.toVector3()).toBlockPoint();
        BlockVector3 posSize3 = BlockVector3.at(-posSize2.getBlockX(), 0, -posSize2.getBlockZ());
        BlockVector3 pos = wPosition.add(posSize3);

        Operation operation = clipboardHolder
                .createPaste(destination)
                .to(pos)
                .ignoreAirBlocks(true)
                // configure here
                .build();
        return operation;
    }

    // 設計図を仮設置
    public void updateFakeSchematic(Player wPlayer, BlockVector3 wPosition, Clipboard clipboard, int yawInt, boolean isVisible) {
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
            Extent fakeExtent = isVisible
                    ? new FakeExtent(wPlayer.getWorld(), wPlayer)
                    : new DummyExtent(wPlayer.getWorld(), wPlayer,
                    FuzzyBlockState.builder().type(BlockTypes.GLASS).build());
            Extent fakeChangeExtent = new ChangeSetExtent(fakeExtent, lastChangeSet);
            Operations.completeBlindly(createPlaceOperation(wPosition, clipboard, fakeChangeExtent, yawInt));
        }
    }

    // 設計図を設置
    public void placeSchematic(Player wPlayer, BlockVector3 wPosition, Clipboard clipboard, int yawInt) throws WorldEditException {
        if (clipboard != null && wPosition != null) {
            // プレイヤーセッション
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wPlayer);

            // クリップボードからスケマティックを設置
            try (EditSession editSession = session.createEditSession(wPlayer)) {
                Operations.complete(createPlaceOperation(wPosition, clipboard, editSession, yawInt));
                // Undo履歴に記録
                session.remember(editSession);
            }
        }
    }
}
