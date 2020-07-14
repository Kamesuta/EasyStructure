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
                for (final Player player : EasyStructure.INSTANCE.getServer().getOnlinePlayers()) {
                    onEffect(player);
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

        // 向き
        int yawInt = ESUtils.getYawInt(wPlayer);

        // 状態が変わった
        boolean stateSame = yawInt == essession.lastYawInt
                && essession.yawOffsetInt == essession.lastYawOffsetInt
                && Objects.equals(uuid, essession.lastUuid)
                && Objects.equals(wPosition, essession.lastPosition);

        // 状態が変わったら時計をリセット
        if (!stateSame)
            essession.timer.reset();

        // 時計
        double span = 2750;
        boolean visible = ((essession.timer.getTime() % span) / span) < 0.8;

        // 同じ状態なら更新しない
        if (stateSame && Objects.equals(visible, essession.lastVisible))
            return;
        essession.lastUuid = uuid;
        essession.lastPosition = wPosition;
        essession.lastVisible = visible;
        essession.lastYawInt = yawInt;
        essession.lastYawOffsetInt = essession.yawOffsetInt;

        // スケマティックをクリップボードに読み込み
        final Clipboard clipboard = essession.getClipboardCachedFromId(uuid);

        // フェイクブロック更新
        essession.updateFakeSchematic(wPlayer, wPosition, clipboard, yawInt - essession.yawOffsetInt, visible);
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
        essession.yawOffsetInt = (essession.yawOffsetInt + 1) % 4;
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

            // 向き
            float yaw = wPlayer.getLocation().getYaw();
            int yawInt = (int) ((((yaw - 135f) % 360f + 360f) % 360f) / 90f);

            // スケマティックをクリップボードに読み込み
            final Clipboard clipboard = essession.getClipboardCachedFromId(uuid);
            if (clipboard == null) {
                player.sendMessage("この設計図はもう使えません。(原因: 鯖のファイルいじれる人が設計図を消した。)");
                return;
            }

            // フェイクブロック更新
            essession.placeSchematic(wPlayer, wPosition, clipboard, yawInt - essession.yawOffsetInt);

            // アイテム名取得
            String title = ChatColor.stripColor(player.getInventory().getItemInMainHand().getItemMeta().getDisplayName());

            // 設定でONのときログ出力
            if (EasyStructure.INSTANCE.getConfig().getBoolean(Config.SETTING_PLACE_LOG))
                Log.log.log(Level.INFO, String.format("%s placed schematic ( %s : %s ).", player.getName(), title, uuid));

            // アクションバー
            player.sendActionBar("§" + Integer.toHexString(random.nextInt(16)) + "設計図を設置しました。");

        } catch (WorldEditException e) {
            Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
            player.sendMessage("WorldEditエラー: " + e.getMessage());
        }
    }
}
