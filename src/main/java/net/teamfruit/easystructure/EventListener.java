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
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.FileInputStream;
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

    private void onEffect(Player player) {
        // ブレイズロッドなら
        final ItemStack itemMain = player.getInventory().getItemInMainHand();
        if (itemMain.getType() != Material.BLAZE_ROD)
            return;

        BaseItemStack itemStack = BukkitAdapter.adapt(itemMain);

        // アイテムからUUID取得
        String uuid = null;
        if (itemStack.hasNbtData()) {
            CompoundTag tag = itemStack.getNbtData();
            Tag esTag = tag.getValue().get("es");
            if (esTag instanceof CompoundTag) {
                uuid = ((CompoundTag) esTag).getString("id");
            }
        }

        if (uuid == null)
            return;

        // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
        RayTraceResult rayResult = player.rayTraceBlocks(plugin.getConfig().getInt(Config.SETTING_PLACE_RANGE), FluidCollisionMode.NEVER);

        if (rayResult == null)
            return;

        Block hitBlock = rayResult.getHitBlock();
        BlockFace hitFace = rayResult.getHitBlockFace();

        if (hitBlock == null)
            return;

        // パーティクル表示
        final int colorInt = plugin.getConfig().getInt(Config.SETTING_PARTICLE_COLOR, 0xffffff);
        final Color color = Color.fromRGB(colorInt);
        final int range = plugin.getConfig().getInt(Config.SETTING_PARTICLE_RANGE);
        player.spawnParticle(Particle.REDSTONE, hitBlock.getRelative(hitFace).getLocation().add(.5, .5, .5), 1, 0, 0, 0, new Particle.DustOptions(color, 1));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUse(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        final Player player = event.getPlayer();

        // 権限チェック
        if (!player.hasPermission("es.use"))
            return;

        // ブレイズロッドなら
        final ItemStack itemMain = player.getInventory().getItemInMainHand();
        if (itemMain.getType() != Material.BLAZE_ROD)
            return;

        // プレイヤーが向いている先のブロック (コンフィグで最大範囲指定可能)
        RayTraceResult rayResult = player.rayTraceBlocks(plugin.getConfig().getInt(Config.SETTING_PLACE_RANGE), FluidCollisionMode.NEVER);

        if (rayResult == null)
            return;

        Block hitBlock = rayResult.getHitBlock();
        BlockFace hitFace = rayResult.getHitBlockFace();

        if (hitBlock == null)
            return;

        Action action = event.getAction();

        // 右クリックしたら
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            try {
                AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
                World wWorld = wPlayer.getWorld();
                BlockVector3 wPosition = BukkitAdapter.asBlockVector(hitBlock.getRelative(hitFace).getLocation());

                // アイテム名取得
                String title = itemMain.getItemMeta().getDisplayName();

                BaseItemStack itemStack = BukkitAdapter.adapt(itemMain);

                // アイテムからUUID取得
                String uuid = null;
                if (itemStack.hasNbtData()) {
                    CompoundTag tag = itemStack.getNbtData();
                    Tag esTag = tag.getValue().get("es");
                    if (esTag instanceof CompoundTag) {
                        uuid = ((CompoundTag) esTag).getString("id");
                    }
                }

                if (uuid == null)
                    return;

                // 保存先
                File file = new File(plugin.schematicDirectory, uuid + ".schem");

                if (!file.exists()) {
                    player.sendMessage("この設計図はもう使えません。(原因: 鯖のファイルいじれる人が設計図を消した。)");
                    return;
                }

                // スケマティックをクリップボードに読み込み
                Clipboard clipboard;
                ClipboardFormat format = ClipboardFormats.findByFile(file);
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    clipboard = reader.read();
                }

                // プレイヤーセッション
                LocalSession session = WorldEdit.getInstance()
                        .getSessionManager()
                        .get(wPlayer);

                // クリップボードからスケマティックを設置
                try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(wWorld, -1)) {
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(wPosition)
                            // configure here
                            .build();
                    Operations.complete(operation);
                    // Undo履歴に記録
                    session.remember(editSession);
                }

                // 設定でONのときログ出力
                if (plugin.getConfig().getBoolean(Config.SETTING_PLACE_LOG))
                    Log.log.log(Level.INFO, String.format("%s placed schematic ( %s : %s ).", player.getName(), title, uuid));

                player.sendActionBar("§" + Integer.toHexString(random.nextInt(16)) + "設計図を設置しました。");

            } catch (WorldEditException e) {
                Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
                player.sendMessage("WorldEditエラー: " + e.getMessage());
            } catch (IOException e) {
                Log.log.log(Level.WARNING, "IO Error: ", e);
                player.sendMessage("ロードに失敗しました: " + e.getMessage());
            }
        }
    }
}
