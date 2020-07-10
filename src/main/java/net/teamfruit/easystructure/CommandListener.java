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
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class CommandListener implements CommandExecutor, TabCompleter {
    private final EasyStructure plugin;

    public CommandListener(EasyStructure plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("プレイヤーのみが設計図を作成できます。");
            return true;
        }

        Player player = (Player) sender;

        try {
            AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
            World wWorld = wPlayer.getWorld();

            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wPlayer);

            if (!session.isSelectionDefined(wWorld)) {
                player.sendMessage("範囲が選択されていません。ワールドエディットで範囲を選択してください。");
                return true;
            }

            Region region = session.getSelection(wWorld);

            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

            String uuid = UUID.randomUUID().toString();

            String title = args.length > 0 ? args[0] : "設計図" + StringUtils.substringBefore(uuid, "-");

            CompoundTag tag = CompoundTagBuilder.create()
                    .put("es", CompoundTagBuilder.create()
                            .put("id", new StringTag(uuid))
                            .build()
                    )
                    .put("display", CompoundTagBuilder.create()
                            .put("Name", new StringTag(String.format("{\"text\":\"%s\",\"color\":\"dark_green\",\"italic\":false}", title)))
                            .put("Lore", ListTagBuilder.create(StringTag.class)
                                    .add(new StringTag(String.format("{\"text\":\"%s\",\"color\":\"dark_gray\",\"italic\":false}", uuid)))
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

            player.sendMessage("設計図" + (title != null ? "[" + title + "]" : "") + "を作成しました。");
        } catch (WorldEditException e) {
            Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
            player.sendMessage("WorldEditエラー: " + e.getMessage());
        } catch (IOException e) {
            Log.log.log(Level.WARNING, "IO Error: ", e);
            player.sendMessage("セーブに失敗しました: " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
