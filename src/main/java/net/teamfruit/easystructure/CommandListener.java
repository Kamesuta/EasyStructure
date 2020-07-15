package net.teamfruit.easystructure;

import com.sk89q.jnbt.CompoundTag;
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
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.item.ItemTypes;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.LocaleUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class CommandListener implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(I18n.format("command.error.playeronly"));
            return true;
        }

        Player player = (Player) sender;

        try {
            AbstractPlayerActor wPlayer = BukkitAdapter.adapt(player);
            World wWorld = wPlayer.getWorld();
            ESSession essession = EasyStructure.INSTANCE.sessionManager.get(wPlayer);

            // 引数がないときはアイテム情報表示
            if (args.length == 0) {
                // ブレイズロッドならアイテムからUUID取得
                final String uuid = ESUtils.getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));

                player.sendMessage("");
                player.sendMessage("");

                player.sendMessage(new ComponentBuilder()
                        .append(I18n.format("command.menu.header.start"))
                        .color(ChatColor.GRAY)
                        .append("[")
                        .color(ChatColor.RESET)
                        .append(new TextComponent(
                                new ComponentBuilder(I18n.format("command.menu.title.title"))
                                        .color(ChatColor.BLUE)
                                        .bold(true)
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                                .append(new TextComponent(
                                                        new ComponentBuilder(I18n.format("command.menu.title.title.desc"))
                                                                .color(ChatColor.BLUE)
                                                                .bold(true)
                                                                .create()
                                                ))
                                                .append(I18n.format("command.menu.title.desc"))
                                                .create()
                                        ))
                                        .create()
                        ))
                        .append("]")
                        .color(ChatColor.RESET)
                        .append(I18n.format("command.menu.header.end"))
                        .color(ChatColor.GRAY)
                        .create());

                player.sendMessage("");

                ComponentBuilder component2 = new ComponentBuilder();
                {
                    component2.append(new ComponentBuilder()
                            .append(new TextComponent("["))
                            .append(new TextComponent(new ComponentBuilder(I18n.format("command.menu.button.create"))
                                    .underlined(true)
                                    .color(ChatColor.GREEN)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                            .append(I18n.format("command.menu.button.create.desc.before"))
                                            .append(new TextComponent(
                                                    new ComponentBuilder(I18n.format("command.menu.button.args.name"))
                                                            .color(ChatColor.GRAY)
                                                            .bold(true)
                                                            .create()
                                            ))
                                            .append(I18n.format("command.menu.button.create.desc.after"))
                                            .create()
                                    ))
                                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/es " + I18n.format("command.menu.button.args.name")))
                                    .create()
                            ))
                            .append(new TextComponent("]"))
                            .create()
                    );
                }

                // アイテム名取得
                ComponentBuilder componentA = new ComponentBuilder();
                if (uuid != null) {
                    String title = ChatColor.stripColor(player.getInventory().getItemInMainHand().getItemMeta().getDisplayName());
                    componentA
                            .append(I18n.format("command.menu.button.share"))
                            .color(ChatColor.GREEN)
                            .underlined(true)
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                    .append(I18n.format("command.menu.button.share.unavailable.desc.before"))
                                    .append("[")
                                    .append(new TextComponent(
                                            new ComponentBuilder(title)
                                                    .color(ChatColor.GREEN)
                                                    .create()
                                    ))
                                    .append("]")
                                    .append(I18n.format("command.menu.button.share.unavailable.desc.after"))
                                    .create()
                            ))
                            .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "/es " + uuid + " " + title));
                } else {
                    componentA
                            .append(new TextComponent(
                                    new ComponentBuilder(I18n.format("command.menu.button.share.unavailable"))
                                            .color(ChatColor.RED)
                                            .bold(true)
                                            .create()
                            ))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                    .append(new TextComponent(
                                            new ComponentBuilder(I18n.format("command.menu.button.share.available.desc.before"))
                                                    .color(ChatColor.RED)
                                                    .bold(true)
                                                    .create()
                                    ))
                                    .append(I18n.format("command.menu.button.share.available.desc.after"))
                                    .create()
                            ))
                            .append(I18n.format("command.menu.button.share"))
                            .color(ChatColor.GRAY)
                            .underlined(true);
                }
                component2.append(new ComponentBuilder()
                        .append(new TextComponent(" ["))
                        .append(new TextComponent(componentA.create()))
                        .append(new TextComponent("]"))
                        .create()
                );
                player.sendMessage(component2.create());

                player.sendMessage("");

                player.sendMessage(new ComponentBuilder()
                        .append(I18n.format("command.menu.footer.start"))
                        .color(ChatColor.GRAY)
                        .append("[")
                        .color(ChatColor.RESET)
                        .append(new TextComponent(
                                new ComponentBuilder(I18n.format("command.menu.help"))
                                        .color(ChatColor.GREEN)
                                        .bold(true)
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                                .append(new TextComponent(
                                                        new ComponentBuilder(I18n.format("command.menu.help.desc.title"))
                                                                .color(ChatColor.BLUE)
                                                                .bold(true)
                                                                .create()
                                                ))
                                                .append(I18n.format("command.menu.help.desc.before"))
                                                .append(new TextComponent(
                                                        new ComponentBuilder("//wand")
                                                                .color(ChatColor.GRAY)
                                                                .bold(true)
                                                                .create()
                                                ))
                                                .append(I18n.format("command.menu.help.desc.after"))
                                                .create()
                                        ))
                                        .create()
                        ))
                        .append("]")
                        .color(ChatColor.RESET)
                        .append(I18n.format("command.menu.footer.end"))
                        .color(ChatColor.RESET)
                        .create());

                player.sendMessage("");

                return true;
            }

            // 引数0は名前
            final String title;
            final String uuid;
            if (args.length >= 2) {
                // 引数1はUUID
                uuid = args[0];
                title = ESUtils.getArgs(args, 1);

                if (!essession.isValidId(uuid)) {
                    player.sendMessage(I18n.format("command.error.invalidid"));
                    return true;
                }
            } else {
                title = ESUtils.getArgs(args, 0);

                // プレイヤーセッション
                LocalSession session = WorldEdit.getInstance()
                        .getSessionManager()
                        .get(wPlayer);

                if (!session.isSelectionDefined(wWorld)) {
                    player.sendMessage(new ComponentBuilder()
                            .append(I18n.format("command.error.noselection"))
                            .append("[")
                            .color(ChatColor.RESET)
                            .append(new TextComponent(
                                    new ComponentBuilder(I18n.format("command.menu.help"))
                                            .color(ChatColor.GREEN)
                                            .bold(true)
                                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                                    .append(new TextComponent(
                                                            new ComponentBuilder(I18n.format("command.menu.help.desc.title"))
                                                                    .color(ChatColor.BLUE)
                                                                    .bold(true)
                                                                    .create()
                                                    ))
                                                    .append(I18n.format("command.menu.help.desc.before"))
                                                    .append(new TextComponent(
                                                            new ComponentBuilder("//wand")
                                                                    .color(ChatColor.GRAY)
                                                                    .bold(true)
                                                                    .create()
                                                    ))
                                                    .append(I18n.format("command.menu.help.desc.after"))
                                                    .create()
                                            ))
                                            .create()
                            ))
                            .append("]")
                            .color(ChatColor.RESET)
                            .create()
                    );
                    return true;
                }

                Region region = session.getSelection(wWorld);

                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

                uuid = UUID.randomUUID().toString();

                // 向き
                essession.yawOffsetInt = ESUtils.getYawInt(wPlayer);

                // スケマティックをクリップボードに保存
                try (EditSession editSession = WorldEdit.getInstance()
                        .getEditSessionFactory()
                        .getEditSession(wWorld, -1)) {
                    ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                            editSession, region, clipboard, region.getMinimumPoint()
                    );
                    // configure here
                    Operations.complete(forwardExtentCopy);
                }

                // 保存先
                File file = new File(EasyStructure.INSTANCE.schematicDirectory, uuid + ".schem");

                // クリップボードをファイルに保存
                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
                    writer.write(clipboard);
                }
            }

            // NBTにUUIDをセット (見た目もセット)
            CompoundTag tag = ESUtils.createWandItem(uuid, title);

            // アイテムを渡す
            BaseItemStack itemStack = new BaseItemStack(ItemTypes.BLAZE_ROD, tag, 1);
            wPlayer.giveItem(itemStack);

            player.sendMessage(I18n.format("command.success.created", (title != null ? "[" + title + "]" : "")));

        } catch (WorldEditException e) {
            Log.log.log(Level.WARNING, "WorldEdit Error: ", e);
            player.sendMessage(I18n.format("es.error.worldedit", e.getMessage()));
        } catch (IOException e) {
            Log.log.log(Level.WARNING, "IO Error: ", e);
            player.sendMessage(I18n.format("es.error.ioerror", e.getMessage()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1 || args.length == 2) {
            Locale locale = LocaleUtils.toLocale(I18n.format("java.locale"));
            String date = new SimpleDateFormat(I18n.format("command.completion.date"), locale).format(new Date());
            completions.add(I18n.format("command.completion.text.withname", sender.getName(), date));
            completions.add(I18n.format("command.completion.text.dateonly", date));
        }
        return completions;
    }
}
