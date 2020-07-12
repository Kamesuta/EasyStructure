package net.teamfruit.easystructure;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.changeset.BlockOptimizedHistory;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.item.ItemTypes;
import net.teamfruit.easystructure.fakepaste.FakeExtent;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

public class EventListener implements Listener {
    private final EasyStructure plugin;
    private final Random random = new Random();

    public EventListener(EasyStructure plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player player : plugin.getServer().getOnlinePlayers()) {
                    onEffect(player);
                }
            }
        }.runTaskTimer(this.plugin, 3, 3);
    }

    // アイテムからUUIDを取得
    @Nullable
    private String getWandId(BaseItemStack itemStack) {
        if (itemStack.getType() == ItemTypes.BLAZE_ROD && itemStack.hasNbtData()) {
            CompoundTag tag = itemStack.getNbtData();
            Tag esTag = tag.getValue().get("es");
            if (esTag instanceof CompoundTag) {
                return ((CompoundTag) esTag).getString("id");
            }
        }
        return null;
    }

    // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
    @Nullable
    private BlockVector3 getPlaceLocation(Player player) {
        RayTraceResult rayResult = player.rayTraceBlocks(plugin.getConfig().getInt(Config.SETTING_PLACE_RANGE), FluidCollisionMode.NEVER);

        if (rayResult == null)
            return null;

        Block hitBlock = rayResult.getHitBlock();
        BlockFace hitFace = rayResult.getHitBlockFace();

        if (hitBlock == null)
            return null;

        return BukkitAdapter.asBlockVector(hitBlock.getRelative(hitFace).getLocation());
    }

    private void onEffect(Player player) {
        try {
            AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
            ESSession essession = plugin.sessionManager.get(wPlayer);

            // ブレイズロッドならアイテムからUUID取得
            final String uuid = getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));
            if (uuid == null)
                return;

            // スケマティックをクリップボードに読み込み
            final Clipboard clipboard = essession.getClipboardFromId(uuid);
            if (clipboard == null) {
                player.sendMessage("この設計図はもう使えません。(原因: 鯖のファイルいじれる人が設計図を消した。)");
                return;
            }

            // Fakeロールバック
            if (essession.lastChangeSet != null) {
                Extent fakeExtent = new FakeExtent(wPlayer.getWorld(), wPlayer);
                UndoContext context = new UndoContext();
                context.setExtent(fakeExtent);
                Operations.completeBlindly(ChangeSetExecutor.createUndo(essession.lastChangeSet, context));
            }

            // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
            BlockVector3 wPosition = getPlaceLocation(player);
            if (wPosition == null)
                return;

            // プレイヤーセッション
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wPlayer);

            // フェイクブロック送信
            essession.lastChangeSet = new BlockOptimizedHistory();
            Extent fakeExtent = new FakeExtent(wPlayer.getWorld(), wPlayer);
            Extent fakeChangeExtent = new ChangeSetExtent(fakeExtent, essession.lastChangeSet);
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(fakeChangeExtent)
                    .to(wPosition)
                    .ignoreAirBlocks(true)
                    // configure here
                    .build();
            Operations.complete(operation);

            // パーティクル表示
            final int colorInt = plugin.getConfig().getInt(Config.SETTING_PARTICLE_COLOR, 0xffffff);
            final Color color = Color.fromRGB(colorInt);
            final int range = plugin.getConfig().getInt(Config.SETTING_PARTICLE_RANGE);
            player.spawnParticle(Particle.REDSTONE, wPosition.getX() + .5, wPosition.getY() + .5, wPosition.getZ() + .5, 1, 0, 0, 0, new Particle.DustOptions(color, 1));
        } catch (WorldEditException e) {
            Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
            player.sendMessage("WorldEditエラー: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUse(final PlayerInteractEvent event) {
        Action action = event.getAction();
        final Player player = event.getPlayer();

        // 右クリックしたら
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR)
            return;

        // 右手
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        try {
            AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
            ESSession essession = plugin.sessionManager.get(wPlayer);

            // 権限チェック
            if (!wPlayer.hasPermission("es.use"))
                return;

            // ブレイズロッドならアイテムからUUID取得
            final String uuid = getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));
            if (uuid == null)
                return;

            // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
            BlockVector3 wPosition = getPlaceLocation(player);
            if (wPosition == null)
                return;

            // スケマティックをクリップボードに読み込み
            final Clipboard clipboard = essession.getClipboardFromId(uuid);
            if (clipboard == null) {
                player.sendMessage("この設計図はもう使えません。(原因: 鯖のファイルいじれる人が設計図を消した。)");
                return;
            }

            // プレイヤーセッション
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wPlayer);

            // クリップボードからスケマティックを設置
            try (EditSession editSession = session.createEditSession(wPlayer)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(wPosition)
                        .ignoreAirBlocks(true)
                        // configure here
                        .build();
                Operations.complete(operation);
                // Undo履歴に記録
                session.remember(editSession);
            }

            // アイテム名取得
            String title = player.getInventory().getItemInMainHand().getItemMeta().getDisplayName();

            // 設定でONのときログ出力
            if (plugin.getConfig().getBoolean(Config.SETTING_PLACE_LOG))
                Log.log.log(Level.INFO, String.format("%s placed schematic ( %s : %s ).", player.getName(), title, uuid));

            // アクションバー
            player.sendActionBar("§" + Integer.toHexString(random.nextInt(16)) + "設計図を設置しました。");

        } catch (WorldEditException e) {
            Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
            player.sendMessage("WorldEditエラー: " + e.getMessage());
        }
    }
}
