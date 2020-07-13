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
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandListener implements CommandExecutor, TabCompleter {
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
            ESSession essession = EasyStructure.INSTANCE.sessionManager.get(wPlayer);

            // 引数がないときはアイテム情報表示
            if (args.length == 0) {
                // ブレイズロッドならアイテムからUUID取得
                final String uuid = ESUtils.getWandId(wPlayer.getItemInHand(HandSide.MAIN_HAND));

                player.sendMessage("");
                player.sendMessage("");

                player.sendMessage(new ComponentBuilder()
                        .append("======")
                        .color(ChatColor.GRAY)
                        .append("[")
                        .color(ChatColor.RESET)
                        .append(new TextComponent(
                                new ComponentBuilder("EasyStructureメニュー")
                                        .color(ChatColor.BLUE)
                                        .bold(true)
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                                .append(new TextComponent(
                                                        new ComponentBuilder("猿でもわかるコピペプラグインへようこそ！。 ")
                                                                .color(ChatColor.BLUE)
                                                                .bold(true)
                                                                .create()
                                                ))
                                                .append("このプラグインは設計図(ブレイズロッド)を振るだけで建物がホイホイとできるよ。 ")
                                                .append("下のボタンから、設計図の作成や、設計図の共有ができるよ。 ")
                                                .append("また、Discord等に貼ってあるコマンドをコピーすると設計図を入手できるよ。 ")
                                                .create()
                                        ))
                                        .create()
                        ))
                        .append("]")
                        .color(ChatColor.RESET)
                        .append("======")
                        .color(ChatColor.GRAY)
                        .create());

                player.sendMessage("");

                ComponentBuilder component2 = new ComponentBuilder();
                {
                    component2.append(new ComponentBuilder()
                            .append(new TextComponent("["))
                            .append(new TextComponent(new ComponentBuilder("WEの範囲から作成")
                                    .underlined(true)
                                    .color(ChatColor.GREEN)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                            .append("WorldEditの選択範囲から設計図を作成します。 ")
                                            .append("クリックするとコマンドが入力されるので、")
                                            .append(new TextComponent(
                                                    new ComponentBuilder("<名前>")
                                                            .color(ChatColor.GRAY)
                                                            .bold(true)
                                                            .create()
                                            ))
                                            .append("の部分を好きな名前に置き換えてください。 ")
                                            .create()
                                    ))
                                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/es <名前>"))
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
                            .append("設計図をDiscord等で共有")
                            .color(ChatColor.GREEN)
                            .underlined(true)
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                    .append("設計図[")
                                    .append(new TextComponent(
                                            new ComponentBuilder(title)
                                                    .color(ChatColor.GREEN)
                                                    .create()
                                    ))
                                    .append("]を召喚できるコマンドをクリップボードにコピーします。 ")
                                    .append("Discord等にコピーされたコマンドを貼り付けてください。 ")
                                    .append("クリックしても反応がないですが、ちゃんとコピーされているのでご安心ください。 ")
                                    .create()
                            ))
                            .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "/es " + uuid + " " + title));
                } else {
                    componentA
                            .append(new TextComponent(
                                    new ComponentBuilder("✕")
                                            .color(ChatColor.RED)
                                            .bold(true)
                                            .create()
                            ))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                    .append(new TextComponent(
                                            new ComponentBuilder("設計図を手に持ってください。 ")
                                                    .color(ChatColor.RED)
                                                    .bold(true)
                                                    .create()
                                    ))
                                    .append("手持ちの設計図を召喚できるコマンドをクリップボードにコピーします。 ")
                                    .create()
                            ))
                            .append("設計図をDiscord等で共有")
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
                        .append("================================")
                        .color(ChatColor.GRAY)
                        .append("[")
                        .color(ChatColor.RESET)
                        .append(new TextComponent(
                                new ComponentBuilder("?")
                                        .color(ChatColor.GREEN)
                                        .bold(true)
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                                .append(new TextComponent(
                                                        new ComponentBuilder("WorldEditでの範囲の選択方法がわからない人へ。 ")
                                                                .color(ChatColor.BLUE)
                                                                .bold(true)
                                                                .create()
                                                ))
                                                .append(new TextComponent(
                                                        new ComponentBuilder("//wand")
                                                                .color(ChatColor.GRAY)
                                                                .bold(true)
                                                                .create()
                                                ))
                                                .append("で木の斧を取り出します。 ")
                                                .append("木の斧をもって2つのブロックにそれぞれ左クリック、右クリックをします。 ")
                                                .append("この時、2つのブロックの間の直方体が選択範囲になります。 ")
                                                .append("選択ができたら、上にあるボタンを押して設計図を作ってください。 ")
                                                .create()
                                        ))
                                        .create()
                        ))
                        .append("]")
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
                    player.sendMessage("設計図IDが不正です。IDに間違いがないか確認してください。");
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
                            .append("範囲が選択されていません。WorldEditで範囲を選択してください。")
                            .append("[")
                            .color(ChatColor.RESET)
                            .append(new TextComponent(
                                    new ComponentBuilder("?")
                                            .color(ChatColor.GREEN)
                                            .bold(true)
                                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder()
                                                    .append(new TextComponent(
                                                            new ComponentBuilder("WorldEditでの範囲の選択方法がわからない人へ。 ")
                                                                    .color(ChatColor.BLUE)
                                                                    .bold(true)
                                                                    .create()
                                                    ))
                                                    .append(new TextComponent(
                                                            new ComponentBuilder("//wand")
                                                                    .color(ChatColor.GRAY)
                                                                    .bold(true)
                                                                    .create()
                                                    ))
                                                    .append("で木の斧を取り出します。 ")
                                                    .append("木の斧をもって2つのブロックにそれぞれ左クリック、右クリックをします。 ")
                                                    .append("この時、2つのブロックの間の直方体が選択範囲になります。 ")
                                                    .append("選択ができたら、上にあるボタンを押して設計図を作ってください。 ")
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
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add(sender.getName() + "の建築" + new SimpleDateFormat("M月d日(E)H時m分").format(new Date()));
            completions.add("設計図" + new SimpleDateFormat("M月d日(E)H時m分").format(new Date()));
        } else if (args.length == 2) {
            String[] schems = EasyStructure.INSTANCE.schematicDirectory.list();
            if (schems != null) {
                List<String> files = Arrays.stream(schems)
                        .filter(e -> e.endsWith(".schem"))
                        .map(e -> StringUtils.substringBeforeLast(e, ".schem"))
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], files, completions);
                Collections.sort(completions);
            }
        }
        return completions;
    }
}
