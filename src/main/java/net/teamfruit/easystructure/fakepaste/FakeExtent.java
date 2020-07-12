package net.teamfruit.easystructure.fakepaste;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class FakeExtent extends AbstractDelegateExtent {
    private final Player player;

    public FakeExtent(Extent extent, Player player) {
        super(extent);
        this.player = player;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        player.sendFakeBlock(position, block);
        return true;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return false;
    }
}
