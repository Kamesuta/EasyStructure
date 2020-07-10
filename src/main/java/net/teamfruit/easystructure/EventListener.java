package net.teamfruit.easystructure;

import com.sk89q.jnbt.*;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.apache.commons.lang.StringUtils;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class EventListener implements Listener {
    private final EasyStructure plugin;
    private final Random random = new Random();

    public EventListener(EasyStructure plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                onEffect();
            }
        }.runTaskTimer(this.plugin, 3, 3);
    }

    private void onEffect() {
        for (final Player player : this.plugin.getServer().getOnlinePlayers()) {
            final ItemStack itemMain = player.getInventory().getItemInMainHand();
            if (itemMain.getType() != Material.BLAZE_ROD)
                continue;

            RayTraceResult rayResult = player.rayTraceBlocks(32, FluidCollisionMode.NEVER);

            if (rayResult == null)
                continue;

            Block hitBlock = rayResult.getHitBlock();
            BlockFace hitFace = rayResult.getHitBlockFace();

            if (hitBlock == null)
                continue;

            final int colorInt = plugin.getConfig().getInt(Config.SETTING_PARTICLE_COLOR, 0xffffff);
            final Color color = Color.fromRGB(colorInt);
            final int range = plugin.getConfig().getInt(Config.SETTING_PARTICLE_RANGE);
            /*
            if (range > 0) {
                for (final Player other : Bukkit.getOnlinePlayers())
                    if (other.getLocation().distance(player.getLocation()) <= range)
                        for (final Location block : blocks)
                            this.nativemc.spawnParticles(other, block, color_r / 255f, color_g / 255f, color_b / 255f);
            } else
                for (final Location block : blocks)
                    this.nativemc.spawnParticles(player, block, color_r / 255f, color_g / 255f, color_b / 255f);
            */
            player.spawnParticle(Particle.REDSTONE, hitBlock.getRelative(hitFace).getLocation().add(.5, .5, .5), 1, 0, 0, 0, new Particle.DustOptions(color, 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUse(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        final Player player = event.getPlayer();

        if (!player.hasPermission("es.use"))
            return;

        final ItemStack itemMain = player.getInventory().getItemInMainHand();
        if (itemMain.getType() != Material.BLAZE_ROD)
            return;

        RayTraceResult rayResult = player.rayTraceBlocks(32, FluidCollisionMode.NEVER);

        if (rayResult == null)
            return;

        Block hitBlock = rayResult.getHitBlock();
        BlockFace hitFace = rayResult.getHitBlockFace();

        if (hitBlock == null)
            return;

        if (ActionResult.sendResultMessage(onPlayerUse(player, itemMain, event.getAction(), hitBlock, hitFace), player))
            event.setCancelled(true);
    }

    private ActionResult onPlayerUse(final Player player, ItemStack itemMain, final Action action, final Block target, final BlockFace hitFace) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            try {
                AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
                World wWorld = wPlayer.getWorld();
                BlockVector3 wPosition = BukkitAdapter.asBlockVector(target.getRelative(hitFace).getLocation());

                Clipboard clipboard;

                BaseItemStack itemStack = BukkitAdapter.adapt(itemMain);

                String uuid = null;
                if (itemStack.hasNbtData()) {
                    CompoundTag tag = itemStack.getNbtData();
                    Tag esTag = tag.getValue().get("es");
                    if (esTag instanceof CompoundTag) {
                        uuid = ((CompoundTag) esTag).getString("id");
                    }
                }

                if (uuid == null)
                    return ActionResult.success();

                File file = new File(plugin.schematicDirectory, uuid + ".schem");

                if (!file.exists()) {
                    player.sendMessage("この設計図はもう使えません。(原因: 鯖のファイルいじれる人が設計図を消した。)");
                    return ActionResult.error();
                }

                ClipboardFormat format = ClipboardFormats.findByFile(file);
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    clipboard = reader.read();
                }

                try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(wWorld, -1)) {
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(wPosition)
                            // configure here
                            .build();
                    Operations.complete(operation);
                }

                player.sendActionBar("§" + Integer.toHexString(random.nextInt(16)) + "設計図を設置しました。");
            } catch (WorldEditException e) {
                Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
                player.sendMessage("WorldEditエラー: " + e.getMessage());
                return ActionResult.error("WorldEditエラー: ", e.getMessage());
            } catch (IOException e) {
                Log.log.log(Level.WARNING, "IO Error: ", e);
                player.sendMessage("ロードに失敗しました: " + e.getMessage());
                return ActionResult.error("WorldEditエラー: ", e.getMessage());
            }

            return ActionResult.success();
        }

        return ActionResult.success();
    }
}
