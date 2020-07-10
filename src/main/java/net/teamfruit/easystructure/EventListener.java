package net.teamfruit.easystructure;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.jnbt.ListTagBuilder;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class EventListener implements Listener {
    private final EasyStructure plugin;

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
            player.spawnParticle(Particle.REDSTONE, hitBlock.getLocation().clone().add(hitFace.getDirection()).add(.5, .5, .5), 1, 0, 0, 0, new Particle.DustOptions(color, 1));
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

    private ActionResult onPlayerUse(final Player player, ItemStack itemMain, final Action action, final Block target, final BlockFace face) {
        if (player.isSneaking() && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            try {
                AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
                World wWorld = wPlayer.getWorld();

                LocalSession session = WorldEdit.getInstance()
                        .getSessionManager()
                        .get(wPlayer);

                if (!session.isSelectionDefined(wWorld)) {
                    player.sendMessage("NO SELECTION");
                    return ActionResult.error();
                }

                Region region = session.getSelection(wWorld);

                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

                String uuid = UUID.randomUUID().toString();

                CompoundTag tag = CompoundTagBuilder.create()
                        .put("es", CompoundTagBuilder.create()
                                .put("id", new StringTag(uuid))
                                .build()
                        )
                        .put("display", CompoundTagBuilder.create()
                                .put("Lore", ListTagBuilder.create(StringTag.class)
                                        .add(new StringTag(String.format("{\"text\":\"%s\",\"color\":\"gray\",\"italic\":false}", uuid)))
                                        .build()
                                )
                                .build()
                        )
                        .build();
                BaseItemStack itemStack = new BaseItemStack(ItemTypes.BLAZE_ROD, tag, 1);

                wPlayer.giveItem(itemStack);

                try (EditSession editSession = WorldEdit.getInstance()
                        .getEditSessionFactory()
                        .getEditSession(wWorld, -1)) {
                    ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                            editSession, region, clipboard, region.getMinimumPoint()
                    );
                    // configure here
                    Operations.complete(forwardExtentCopy);
                }

                File file = new File(plugin.schematicDirectory, uuid + ".schem");

                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
                    writer.write(clipboard);
                }

                player.sendMessage("CREATED");
            } catch (WorldEditException e) {
                ActionResult.error("WorldEdit Error: ", e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return ActionResult.success();
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            player.sendMessage("PLACE: " + target);
            return ActionResult.success();
        }

        return ActionResult.success();
    }
}
