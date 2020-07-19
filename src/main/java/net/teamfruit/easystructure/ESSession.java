package net.teamfruit.easystructure;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.reorder.ChunkBatchingExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import net.teamfruit.easystructure.fakepaste.FakeExtent;
import net.teamfruit.easystructure.fakepaste.RealExtent;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

public class ESSession {
    // Schematicキャッシュ
    public String currentSchematic;
    public Clipboard currentClipboard;

    // Stateキャッシュ
    public String lastUuid;
    public PasteState lastPaste;
    public Timer timer = new Timer();
    public int yawOffsetInt;

    // バッチキャッシュ
    public int lastEntityId;
    public World lastWorld;
    public ChunkBatchingExtent lastBatchExtent;

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
        if (uuid == null)
            return false;

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
        if (uuid.equals(currentSchematic))
            clipboard = currentClipboard;
        else {
            // IDの変更チェック用
            currentSchematic = uuid;

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
            currentClipboard = clipboard;
        }
        return clipboard;
    }

    public static class PasteState {
        private String uuid;
        private BlockVector3 wPosition;
        private Clipboard clipboard;
        private int yawInt;
        private int yawOffsetInt;

        private PasteState(String uuid, BlockVector3 wPosition, Clipboard clipboard, int yawInt, int yawOffsetInt) {
            this.uuid = uuid;
            this.wPosition = wPosition;
            this.clipboard = clipboard;
            this.yawInt = yawInt;
            this.yawOffsetInt = yawOffsetInt;
        }

        // 引数がnullの場合はnullをreturn
        public static PasteState createOrNull(String uuid, BlockVector3 wPosition, Clipboard clipboard, int yawInt, int yawOffsetInt) {
            if (uuid == null || wPosition == null || clipboard == null)
                return null;
            return new PasteState(uuid, wPosition, clipboard, yawInt, yawOffsetInt);
        }

        // 貼り付け操作
        @Nullable
        public Operation createPlaceOperation(Extent destination) {
            ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
            Transform transformNormal = new AffineTransform().rotateY(yawInt * -90.0);
            Transform transformOffset = new AffineTransform().rotateY(yawOffsetInt * 90.0);
            Transform transform = transformNormal.combine(transformOffset);
            clipboardHolder.setTransform(transform);

            BlockVector3 posSize0 = clipboard.getDimensions();
            BlockVector3 posSize1 = BlockVector3.at(posSize0.getBlockX() / 2, 0, posSize0.getBlockZ() / 2);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PasteState that = (PasteState) o;
            return yawInt == that.yawInt &&
                    yawOffsetInt == that.yawOffsetInt &&
                    Objects.equals(uuid, that.uuid) &&
                    Objects.equals(wPosition, that.wPosition) &&
                    Objects.equals(clipboard, that.clipboard);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, wPosition, clipboard, yawInt, yawOffsetInt);
        }
    }

    // 設計図を仮設置
    public void updateFakeSchematic(Player wPlayer, PasteState paste) {
        World wWorld = wPlayer.getWorld();
        int entityId = wPlayer instanceof BukkitPlayer ? ((BukkitPlayer) wPlayer).getPlayer().getEntityId() : 0;
        if (entityId != lastEntityId || !Objects.equals(wWorld, lastWorld)) {
            lastEntityId = entityId;
            lastWorld = wWorld;
            FakeExtent fakeExtent = new FakeExtent(wWorld, wPlayer);
            lastBatchExtent = new ChunkBatchingExtent(fakeExtent);
        }

        // Fakeロールバック
        if (lastPaste != null) {
            RealExtent realExtent = new RealExtent(lastBatchExtent);
            Operations.completeBlindly(lastPaste.createPlaceOperation(realExtent));
        }

        // フェイクブロック送信
        if (paste != null) {
            Operations.completeBlindly(paste.createPlaceOperation(lastBatchExtent));
        }

        // 変更を適用
        if (lastBatchExtent.commitRequired())
            Operations.completeBlindly(lastBatchExtent.commit());

        lastPaste = paste;
    }

    // 設計図を設置
    public void placeSchematic(Player wPlayer, PasteState paste) throws WorldEditException {
        if (paste != null) {
            // プレイヤーセッション
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wPlayer);

            // クリップボードからスケマティックを設置
            try (EditSession editSession = session.createEditSession(wPlayer)) {
                Operations.complete(paste.createPlaceOperation(editSession));
                // Undo履歴に記録
                session.remember(editSession);
            }
        }
    }
}
