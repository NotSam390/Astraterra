package com.astraterra.mod.registry;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.worldgen.structure.CraterPiece;
import com.astraterra.mod.worldgen.structure.CraterStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Structure types (the Java code) and their pieces. The actual structures
 * (sizes, blocks, which biome) are JSON in data/astraterra/worldgen/structure/&lt;planet&gt;/.
 *
 * Registered through DeferredRegister like everything else. Registering into
 * BuiltInRegistries from common setup (the old way) runs after the registries
 * are frozen and is not reliable.
 */
public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Astraterra.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Astraterra.MODID);

    /** One crater type for every planet: its blocks come from the structure JSON. */
    public static final RegistryObject<StructureType<CraterStructure>> CRATER =
            STRUCTURE_TYPES.register("crater", () -> () -> CraterStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CRATER_PIECE =
            STRUCTURE_PIECES.register("crater_piece", () -> (StructurePieceType) CraterPiece::new);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECES.register(eventBus);
    }
}
