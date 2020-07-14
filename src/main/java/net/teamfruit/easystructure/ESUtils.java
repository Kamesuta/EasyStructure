package net.teamfruit.easystructure;

import com.sk89q.jnbt.*;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import javax.annotation.Nullable;

public class ESUtils {
    // アイテムからUUIDを取得
    @Nullable
    public static String getWandId(BaseItemStack itemStack) {
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
    public static BlockVector3 getPlaceLocation(Player player) {
        RayTraceResult rayResult = player.rayTraceBlocks(EasyStructure.INSTANCE.getConfig().getInt(Config.SETTING_PLACE_RANGE), FluidCollisionMode.NEVER);

        if (rayResult == null)
            return null;

        Block hitBlock = rayResult.getHitBlock();
        BlockFace hitFace = rayResult.getHitBlockFace();

        if (hitBlock == null)
            return null;

        return BukkitAdapter.asBlockVector(hitBlock.getRelative(hitFace).getLocation());
    }

    // 引数からテキストを取得
    public static String getArgs(String[] args, int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = num; i < args.length; i++)
            sb.append(args[i]).append(" ");
        return sb.toString().trim();
    }

    // 設計図作成
    public static CompoundTag createWandItem(String uuid, String title) {
        return CompoundTagBuilder.create()
                .put("es", CompoundTagBuilder.create()
                        .put("id", new StringTag(uuid))
                        .build()
                )
                .put("display", CompoundTagBuilder.create()
                        .put("Name", new StringTag(String.format("{\"text\":\"%s\",\"color\":\"dark_green\",\"italic\":false}", title)))
                        .put("Lore", ListTagBuilder.create(StringTag.class)
                                .add(new StringTag(String.format("{\"text\":\"%s\",\"color\":\"dark_gray\",\"italic\":false}", "設計図")))
                                .add(new StringTag(String.format("{\"text\":\"%s\",\"color\":\"dark_gray\",\"italic\":false}", uuid)))
                                .build()
                        )
                        .build()
                )
                .build();
    }

    // 向きの方角
    public static int getYawInt(com.sk89q.worldedit.entity.Player wPlayer) {
        float yaw = wPlayer.getLocation().getYaw();
        return (int) ((((yaw - 135f) % 360f + 360f) % 360f) / 90f);
    }

    public static int repeatInt(int value, int max) {
        return (value % max + max) % max;
    }
}
