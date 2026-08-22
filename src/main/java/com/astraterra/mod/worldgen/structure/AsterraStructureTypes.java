package com.astraterra.mod.worldgen.structure;

import com.astraterra.mod.Astraterra;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class AsterraStructureTypes {

    public static StructureType<CraterStructure> CRATER;
    public static StructurePieceType CRATER_PIECE;

    public static void register() {
        CRATER = Registry.register(
                BuiltInRegistries.STRUCTURE_TYPE,
                Astraterra.MODID + ":crater",
                () -> CraterStructure.CODEC
        );

        CRATER_PIECE = Registry.register(
                BuiltInRegistries.STRUCTURE_PIECE,
                Astraterra.MODID + ":crater_piece",
                CraterPiece::new
        );
    }
}