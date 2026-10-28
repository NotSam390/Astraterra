package com.astraterra.mod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.astraterra.mod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class CraterStructure extends Structure {

    public static final Codec<CraterStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Structure.settingsCodec(instance),
                    Codec.INT.fieldOf("min_radius").forGetter(s -> s.minRadius),
                    Codec.INT.fieldOf("max_radius").forGetter(s -> s.maxRadius),
                    Codec.INT.fieldOf("min_depth").forGetter(s -> s.minDepth),
                    Codec.INT.fieldOf("max_depth").forGetter(s -> s.maxDepth),
                    Codec.INT.fieldOf("min_rim_height").forGetter(s -> s.minRimHeight),
                    Codec.INT.fieldOf("max_rim_height").forGetter(s -> s.maxRimHeight),
                    BlockState.CODEC.fieldOf("floor_block").forGetter(s -> s.floorBlock),
                    BlockState.CODEC.fieldOf("rock_block").forGetter(s -> s.rockBlock)
            ).apply(instance, CraterStructure::new)
    );

    private final int minRadius;
    private final int maxRadius;
    private final int minDepth;
    private final int maxDepth;
    private final int minRimHeight;
    private final int maxRimHeight;
    /** Covers the bottom of the crater (Moon: moon sand). */
    private final BlockState floorBlock;
    /** Fills under the floor and builds the rim (Moon: crater rock). */
    private final BlockState rockBlock;

    public CraterStructure(StructureSettings settings, int minRadius, int maxRadius,
                           int minDepth, int maxDepth, int minRimHeight, int maxRimHeight,
                           BlockState floorBlock, BlockState rockBlock) {
        super(settings);
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.minRimHeight = minRimHeight;
        this.maxRimHeight = maxRimHeight;
        this.floorBlock = floorBlock;
        this.rockBlock = rockBlock;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, (builder) ->
                generateCrater(context, builder));
    }

    private void generateCrater(GenerationContext context, StructurePiecesBuilder builder) {
        // Randomize crater dimensions within min/max ranges
        java.util.Random rand = new java.util.Random(
                context.chunkPos().x * 73856093L ^ context.chunkPos().z * 19349663L ^ 12345L
        );

        int jitterRange = 6;
        int centerX = context.chunkPos().getMiddleBlockX() + rand.nextInt(jitterRange * 2 + 1) - jitterRange;
        int centerZ = context.chunkPos().getMiddleBlockZ() + rand.nextInt(jitterRange * 2 + 1) - jitterRange;

        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                centerX, centerZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );

        int radius = minRadius + rand.nextInt(maxRadius - minRadius + 1);
        int depth = minDepth + rand.nextInt(maxDepth - minDepth + 1);
        int rimHeight = minRimHeight + rand.nextInt(maxRimHeight - minRimHeight + 1);

        builder.addPiece(new CraterPiece(
                new BlockPos(centerX, surfaceY, centerZ),
                radius, depth, rimHeight,
                floorBlock, rockBlock
        ));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CRATER.get();
    }
}