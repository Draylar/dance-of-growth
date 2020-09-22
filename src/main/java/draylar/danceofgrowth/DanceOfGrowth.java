package draylar.danceofgrowth;

import draylar.danceofgrowth.config.DanceOfGrowthConfig;
import draylar.danceofgrowth.impl.PlayerReader;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DanceOfGrowth implements ModInitializer {

    public static final DanceOfGrowthConfig CONFIG = AutoConfig.register(DanceOfGrowthConfig.class, JanksonConfigSerializer::new).getConfig();

    @Override
    public void onInitialize() {

    }

    public static void onShift(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();

        boolean sprinting = ((PlayerReader) player).wasRecentlySprinting();
        boolean jumping = ((PlayerReader) player).wasRecentlyJumping();
        float mod = 1 * (sprinting ? CONFIG.sprintModifier : 1) * (jumping ? CONFIG.jumpModifier : 1) * (world.isRaining() ? CONFIG.rainModifier : 1);

        // 1/25~ chance to grow a nearby crop by default
        if(world.random.nextInt((int) (CONFIG.chance * mod)) == 0) {

            // Collect valid positions around player
            List<BlockPos> fertilizablePositions = new ArrayList<>();
            BlockPos.iterate(pos.add(-CONFIG.horizontalGrowthRadius, -CONFIG.verticalGrowthRadius, -CONFIG.horizontalGrowthRadius), pos.add(CONFIG.horizontalGrowthRadius, CONFIG.verticalGrowthRadius, CONFIG.horizontalGrowthRadius)).forEach(sPos -> {
                BlockState state = world.getBlockState(sPos);

                // Ensure position can be fertilized
                if(state.getBlock() instanceof Fertilizable && (CONFIG.allowCrops || state.getBlock() instanceof SaplingBlock)) {

                    // Check if grass blocks should be added before potentially adding one
                    if(CONFIG.growGrass || !(state.getBlock() instanceof GrassBlock)) {

                        // Ensure position has room to grow
                        if(((Fertilizable) state.getBlock()).canGrow(world, world.random, sPos, state)) {
                            fertilizablePositions.add(sPos.toImmutable());
                        }
                    }
                }
            });

            // Randomize positions
            Collections.shuffle(fertilizablePositions);

            // Nothing found, return early
            if(fertilizablePositions.isEmpty()) {
                return;
            }

            // Act on each crop
            for(int i = 0; i < Math.min(CONFIG.cropPerSneakMin + world.random.nextInt(CONFIG.cropPerSneakMax), fertilizablePositions.size()); i++) {
                BlockPos plantPos = fertilizablePositions.get(i);
                Fertilizable fertilizable = (Fertilizable) world.getBlockState(plantPos).getBlock();
                fertilizable.grow(world, world.random, plantPos, world.getBlockState(plantPos));

                if(CONFIG.showParticles) {
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, plantPos.getX() + .5, plantPos.getY() + .5, plantPos.getZ() + .5, 50, 0.51, 0.5, 0.5, .2);
                }
            }
        }
    }
}
