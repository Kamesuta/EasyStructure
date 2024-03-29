package net.teamfruit.easystructure;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.HandSide;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

public class EventListener implements Listener {
    private final Random random = new Random();

    public EventListener() {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean bPreviewEnabled = EasyStructure.INSTANCE.getConfig().getBoolean(Config.SETTING_PLACE_PREVIEW);
                if (bPreviewEnabled) {
                    for (final Player player : EasyStructure.INSTANCE.getServer().getOnlinePlayers()) {
                        try {
                            onEffect(player);
                        } catch (Exception e) {
                            Log.log.log(Level.WARNING, "Error while EasyStructure Guide Effect", e);
                        }
                    }
                }
            }
        }.runTaskTimer(EasyStructure.INSTANCE, 0, 1);
    }

    private void onEffect(Player player) {
        AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
        ESSession essession = EasyStructure.INSTANCE.sessionManager.get(wPlayer);

        // ブレイズロッドならアイテムからUUID取得
        final String uuid = ESUtils.getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));

        // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
        BlockVector3 wPosition = ESUtils.getPlaceLocation(player);

        // 時計
        double span = 2750;
        boolean visible = true;// ((essession.timer.getTime() % span) / span) < 0.8;

        // スケマティックをクリップボードに読み込み
        final Clipboard clipboard = essession.getClipboardCachedFromId(uuid);

        // ペースト
        ESSession.PasteState paste = null;
        if (visible)
            paste = ESSession.PasteState.createOrNull(uuid, wPosition, clipboard, ESUtils.getYawInt(wPlayer), essession.yawOffsetInt);

        // 状態が変わった
        boolean stateSame = Objects.equals(paste, essession.lastPaste);

        // 同じ状態なら更新しない
        if (stateSame)
            return;

        // 状態が変わったら時計をリセット
        essession.timer.reset();

        // フェイクブロック更新
        essession.updateFakeSchematic(wPlayer, paste);

        essession.lastUuid = uuid;
        essession.lastPaste = paste;
    }

    @EventHandler
    public void onItemSwap(final PlayerSwapHandItemsEvent event) {
        final Player player = event.getPlayer();
        AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
        ESSession essession = EasyStructure.INSTANCE.sessionManager.get(wPlayer);

        // 権限チェック
        if (!wPlayer.hasPermission("es.use"))
            return;

        // ブレイズロッドならアイテムからUUID取得
        final String uuid = ESUtils.getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));
        if (uuid == null)
            return;

        // 回転
        event.setCancelled(true);
        essession.yawOffsetInt = ESUtils.repeatInt(essession.yawOffsetInt + 1, 4);
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
            ESSession essession = EasyStructure.INSTANCE.sessionManager.get(wPlayer);

            // 権限チェック
            if (!wPlayer.hasPermission("es.use"))
                return;

            // ブレイズロッドならアイテムからUUID取得
            final String uuid = ESUtils.getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));
            if (uuid == null)
                return;

            // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
            BlockVector3 wPosition = ESUtils.getPlaceLocation(player);
            if (wPosition == null)
                return;

            // スケマティックをクリップボードに読み込み
            final Clipboard clipboard = essession.getClipboardCachedFromId(uuid);
            if (clipboard == null) {
                player.sendMessage(I18n.format("action.error.deleted"));
                return;
            }

            // ペースト
            ESSession.PasteState paste = ESSession.PasteState.createOrNull(uuid, wPosition, clipboard, ESUtils.getYawInt(wPlayer), essession.yawOffsetInt);

            // フェイクブロック更新
            essession.placeSchematic(wPlayer, paste);

            // アイテム名取得
            String title = ChatColor.stripColor(player.getInventory().getItemInMainHand().getItemMeta().getDisplayName());

            // 設定でONのときログ出力
            boolean bPlaceLog = EasyStructure.INSTANCE.getConfig().getBoolean(Config.SETTING_PLACE_LOG);
            if (bPlaceLog)
                Log.log.log(Level.INFO, String.format("%s placed schematic ( %s : %s ) to (%d, %d, %d).", player.getName(), title, uuid, wPosition.getBlockX(), wPosition.getBlockY(), wPosition.getBlockZ()));

            // アクションバー
            boolean bShowMessage = EasyStructure.INSTANCE.getConfig().getBoolean(Config.SETTING_PLACE_MESSAGE);
            if (bShowMessage)
                player.sendActionBar(I18n.format("action.success.actionbar", Integer.toHexString(random.nextInt(16))));

            // キャンセル
            event.setCancelled(true);

        } catch (WorldEditException e) {
            Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
            player.sendMessage(I18n.format("es.error.worldedit", e.getMessage()));
        }
    }
}
