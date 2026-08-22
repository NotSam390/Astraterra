package com.astraterra.mod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class CraterPiece extends StructurePiece {

    private final int radius;
    private final int depth;
    private final int rimHeight;
    private final BlockPos center;

    public CraterPiece(BlockPos center, int radius, int depth, int rimHeight, StructureTemplateManager manager) {
        super(AsterraStructureTypes.CRATER_PIECE, 0, makeBoundingBox(center, radius, depth, rimHeight));
        this.center = center;
        this.radius = radius;
        this.depth = depth;
        this.rimHeight = rimHeight;
    }

    public CraterPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AsterraStructureTypes.CRATER_PIECE, tag);
        this.center = new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));
        this.radius = tag.getInt("radius");
        this.depth = tag.getInt("depth");
        this.rimHeight = tag.getInt("rimHeight");
    }

    // Cubic Hermite interpolation between two control points (x0,h0) and (x1,h1)
    // with given tangents m0, m1. Produces a continuous curve through both points
    // that also matches the given slope at each end, so consecutive segments
    // sharing a tangent flow into each other with no kink or flat shelf.
    private static double hermite(double x, double x0, double h0, double m0, double x1, double h1, double m1) {
        double dx = x1 - x0;
        double t = (x - x0) / dx;
        double t2 = t * t;
        double t3 = t2 * t;
        double basisH0 = 2 * t3 - 3 * t2 + 1;
        double basisM0 = t3 - 2 * t2 + t;
        double basisH1 = -2 * t3 + 3 * t2;
        double basisM1 = t3 - t2;
        return basisH0 * h0 + basisM0 * dx * m0 + basisH1 * h1 + basisM1 * dx * m1;
    }

    // Angular harmonics (see postProcess) can push a crater's actual geometry
    // well past its nominal radius/depth/rimHeight at some angles. These pad
    // generously so nothing gets silently clipped by too tight a bounding box
    // or scan window — that clipping is what caused "flat sides" on big craters.
    private static int computeScanRadius(int radius) {
        return (int) Math.ceil(radius * 1.8) + 4;
    }

    private static int computeHeightPadding(int base) {
        return (int) Math.ceil(base * 1.8) + 4;
    }

    // Builds `count` sine harmonics with random amplitude/phase around a base
    // frequency, seeded from the given RandomSource. Returns {freq[], amp[], phase[]}.
    private static double[][] buildHarmonics(RandomSource rnd, int count, double baseFreq, double ampMin, double ampMax) {
        double[] freq = new double[count];
        double[] amp = new double[count];
        double[] phase = new double[count];
        for (int i = 0; i < count; i++) {
            freq[i] = baseFreq + i;
            amp[i] = ampMin + rnd.nextDouble() * (ampMax - ampMin);
            phase[i] = rnd.nextDouble() * Math.PI * 2;
        }
        return new double[][] { freq, amp, phase };
    }

    private static double sumHarmonics(double angle, double[][] harmonics) {
        double[] freq = harmonics[0];
        double[] amp = harmonics[1];
        double[] phase = harmonics[2];
        double sum = 0.0;
        for (int i = 0; i < freq.length; i++) {
            sum += amp[i] * Math.sin(freq[i] * angle + phase[i]);
        }
        return sum;
    }

    private static BoundingBox makeBoundingBox(BlockPos center, int radius, int depth, int rimHeight) {
        int xzPad = computeScanRadius(radius);
        int depthPad = computeHeightPadding(depth);
        int rimPad = computeHeightPadding(rimHeight);
        return new BoundingBox(
                center.getX() - xzPad, center.getY() - depthPad, center.getZ() - xzPad,
                center.getX() + xzPad, center.getY() + rimPad, center.getZ() + xzPad
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("cx", center.getX());
        tag.putInt("cy", center.getY());
        tag.putInt("cz", center.getZ());
        tag.putInt("radius", radius);
        tag.putInt("depth", depth);
        tag.putInt("rimHeight", rimHeight);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pos) {

        int cx = center.getX();
        int cz = center.getZ();

        // smooth per-position noise using a simple value noise approach
        // precompute a noise grid to avoid angle banding
        //
        // IMPORTANT: postProcess is invoked once per CHUNK that overlaps this
        // piece's bounding box. For big craters that's many separate calls for
        // the same crater. The passed-in `random` is chunk-scoped and differs
        // between those calls, so seeding the grid from it made every chunk of
        // a large crater compute a different effectiveRadius/rimSpan -> visible
        // terracing and seams at chunk borders. Seed from the crater's own
        // center instead so the shape is identical no matter which chunk call
        // is currently filling it in.
        long noiseSeed = ((long) cx * 341873128712L) ^ ((long) cz * 132897987541L) ^ 0x9E3779B97F4A7C15L;
        RandomSource noiseRandom = RandomSource.create(noiseSeed);

        int scan = computeScanRadius(radius);
        int size = scan * 2 + 1;
        double[] noiseGrid = new double[size * size];
        for (int i = 0; i < noiseGrid.length; i++) {
            noiseGrid[i] = noiseRandom.nextDouble();
        }

        // Three INDEPENDENT sets of angular harmonics, each with its own seed so
        // they don't correlate with each other:
        //  outline – warps effectiveRadius itself, i.e. where the true crater
        //            edge (lipR) sits at each angle. Shared by both the inner
        //            wall and the rim mound since it's the boundary between them.
        //  inner   – warps crater depth/wall character independently per angle
        //            (some sides deeper/steeper, others shallower) WITHOUT
        //            touching the outer rim at all.
        //  outer   – warps rim height and rim width independently per angle
        //            (some sides taller/wider mounds, others low and narrow)
        //            WITHOUT touching the inner wall at all.
        // Using separate harmonics for inner vs outer is what breaks the
        // "inside is just the outside copy-pasted and flipped" look — the two
        // faces of the rim no longer share the same distortion pattern.
        long outlineSeed = ((long) cx * 668265263L) ^ ((long) cz * 374761393L) ^ 0xB5297A4DL;
        double[][] outlineHarmonics = buildHarmonics(RandomSource.create(outlineSeed), 3, 2, 0.03, 0.08);

        long innerSeed = ((long) cx * 1274126177L) ^ ((long) cz * 1610612741L) ^ 0x2545F4914F6CDD1DL;
        double[][] innerHarmonics = buildHarmonics(RandomSource.create(innerSeed), 3, 2, 0.10, 0.25);

        long outerSeed = ((long) cx * 2654435761L) ^ ((long) cz * 40503L) ^ 0x27D4EB2F165667C5L;
        double[][] outerHarmonics = buildHarmonics(RandomSource.create(outerSeed), 3, 2, 0.10, 0.25);

        // Constrain to current chunk bounds to process only what's loaded
        int chunkStartX = chunkPos.getMinBlockX();
        int chunkEndX = chunkPos.getMaxBlockX();
        int chunkStartZ = chunkPos.getMinBlockZ();
        int chunkEndZ = chunkPos.getMaxBlockZ();

        for (int x = Math.max(cx - scan, chunkStartX); x <= Math.min(cx + scan, chunkEndX); x++) {
            for (int z = Math.max(cz - scan, chunkStartZ); z <= Math.min(cz + scan, chunkEndZ); z++) {

                double dx = x - cx;
                double dz = z - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);

                // sample local surface height for this column
                int localSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                // smooth noise from grid — average nearby cells for smoother result
                int nx = (x - cx + scan);
                int nz = (z - cz + scan);
                double n = 0;
                int samples = 0;
                int smoothRadius = Math.max(2, radius / 8);
                for (int sx = -smoothRadius; sx <= smoothRadius; sx++) {
                    for (int sz = -smoothRadius; sz <= smoothRadius; sz++) {
                        int gx = nx + sx;
                        int gz = nz + sz;
                        if (gx >= 0 && gx < size && gz >= 0 && gz < size) {
                            n += noiseGrid[gx * size + gz];
                            samples++;
                        }
                    }
                }
                n = (samples > 0) ? n / samples : 0.5;

                // angle around the crater, used by all three independent harmonic sets.
                //
                // atan2 is numerically unstable right at the origin — within a few
                // blocks of dist=0, tiny changes in dx/dz swing the angle across
                // almost the whole circle. Since depthHere/rimHeightHere are derived
                // from this angle every column, that instability was making the flat
                // crater floor jump to wildly different heights between neighboring
                // columns near the center — the jagged spike/crack cutting through
                // the middle of the floor. angleFade ramps the wobble in smoothly
                // from 0 at dist=0 to full strength by ~12% of the crater's radius
                // (comfortably inside the flat-floor zone), so the center stays flat
                // and consistent while the asymmetry still applies everywhere it's
                // actually visible.
                double angle = Math.atan2(dz, dx);
                double angleFade = Math.min(1.0, dist / Math.max(1.0, radius * 0.12));
                double outlineWobble = sumHarmonics(angle, outlineHarmonics) * angleFade;
                double innerWobble = sumHarmonics(angle, innerHarmonics) * angleFade;
                double outerWobble = sumHarmonics(angle, outerHarmonics) * angleFade;

                // effective radius (the true crater edge / lipR) — positional noise
                // plus the shared outline lobing. This boundary is common to both
                // the inner wall and the rim mound, so it stays a single value.
                double effectiveRadius = radius * (1.0 + (n - 0.5) * 0.16) * (1.0 + outlineWobble);

                // rim width — also warped independently by outerWobble, so the
                // mound can reach further out on some sides than others, not just
                // stand taller
                double rimSpan = Math.max(3.0, radius * 0.14) * (0.85 + n * 0.3) * (1.0 + outerWobble);

                // Key radii defining the profile, in blocks from center:
                //  floorR    – edge of the flat crater floor
                //  shoulderR – inner wall has risen most of the way back up already
                //  lipR      – the crater's geometric edge (true "rim" line)
                //  peakR     – where the raised material actually peaks (pushed OUTWARD
                //              past the lip, so the mound reads as ejected/lifted soil)
                //  endR      – rim has fully decayed back to original terrain
                double floorR = effectiveRadius * 0.35;
                double shoulderR = effectiveRadius * 0.82;
                double lipR = effectiveRadius;
                double peakR = effectiveRadius + rimSpan * 0.5;
                double endR = effectiveRadius + rimSpan * 1.5;

                // Depth and rim height are also warped independently per angle —
                // this is what actually breaks the "inside looks like the outside
                // flipped" symmetry, since the crater can be deep/steep on one side
                // (innerWobble) while the rim mound is tall/wide on a completely
                // different side (outerWobble); they no longer move together.
                //
                // NOTE: the floor itself (h0 below) intentionally does NOT use this
                // wobble. atan2-based angle becomes ill-defined at tiny distances —
                // adjacent blocks right next to the center can have wildly different
                // angles — so making the floor angle-dependent turned it into a
                // chaotic spike field radiating from the center. The floor stays a
                // flat, angle-independent depth; only the wall (h1 onward, well away
                // from the degenerate point at the center) carries the wobble.
                double depthHere = Math.max(1.0, depth * (1.0 + innerWobble));
                double rimHeightHere = Math.max(0.0, rimHeight * (1.0 + outerWobble));

                // Height offset (relative to localSurface) at each control point.
                // Note lipR is no longer left near zero — that near-flat value at
                // an interior node was exactly what created the shelf/ridge on the
                // inner face of the rim, since the segment on both sides of it was
                // forced flat right where they met it.
                double h0 = -depth * 0.9;          // floorR — flat, angle-independent
                double h1 = -depthHere * 0.15;     // shoulderR
                double h2 = rimHeightHere * 0.45;  // lipR
                double h3 = rimHeightHere;     // peakR
                double h4 = 0.0;               // endR

                // Catmull-Rom tangents: interior points (shoulder/lip/peak) take a
                // slope based on their neighbours so the curve flows continuously
                // through them instead of flattening at each node. Only the two true
                // anchors — flat floor centre and untouched terrain far outside the
                // rim — are pinned to zero slope, since those are the only places
                // that should actually look flat.
                double m0 = 0.0;
                double m1 = (h2 - h0) / (lipR - floorR);
                double m2 = (h3 - h1) / (peakR - shoulderR);
                double m3 = (h4 - h2) / (endR - lipR);
                double m4 = 0.0;

                if (dist <= endR) {
                    double h; // height offset relative to localSurface; negative = carved down

                    if (dist <= floorR) {
                        // flat crater floor
                        h = h0;
                    } else if (dist <= shoulderR) {
                        h = hermite(dist, floorR, h0, m0, shoulderR, h1, m1);
                    } else if (dist <= lipR) {
                        h = hermite(dist, shoulderR, h1, m1, lipR, h2, m2);
                    } else if (dist <= peakR) {
                        h = hermite(dist, lipR, h2, m2, peakR, h3, m3);
                    } else {
                        h = hermite(dist, peakR, h3, m3, endR, h4, m4);
                    }

                    int offset = (int) Math.round(h);
                    int target = localSurface + offset;

                    if (offset < 0) {
                        // carve from surface down to the target depth
                        for (int y = localSurface + 1; y >= target; y--) {
                            BlockPos bp = new BlockPos(x, y, z);
                            if (box.isInside(bp)) {
                                level.setBlock(bp, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }

                        // floor material at the bottom of the carve
                        BlockPos floorPos = new BlockPos(x, target, z);
                        if (box.isInside(floorPos)) {
                            level.setBlock(floorPos, Blocks.GRAY_CONCRETE_POWDER.defaultBlockState(), 2);
                        }

                        // fill any gap between floor and solid ground below
                        for (int y = target - 1; y >= target - 3; y--) {
                            BlockPos bp = new BlockPos(x, y, z);
                            if (box.isInside(bp)) {
                                BlockState bs = level.getBlockState(bp);
                                if (bs.isAir()) {
                                    level.setBlock(bp, Blocks.STONE.defaultBlockState(), 2);
                                }
                            }
                        }
                    } else if (offset > 0) {
                        // build the raised mound up from surface level
                        for (int y = localSurface; y <= target; y++) {
                            BlockPos rimPos = new BlockPos(x, y, z);
                            if (box.isInside(rimPos) && level.getBlockState(rimPos).isAir()) {
                                level.setBlock(rimPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                            }
                        }
                    }
                    // offset == 0 -> leave terrain untouched
                }
            }
        }
    }
}