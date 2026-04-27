package net.frostytrix.fletcherstrestle.block.custom;

import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;

import java.util.Optional;

public class FlaxCropBlock extends CropBlock {
    public static final int FIRST_STAGE_MAX_AGE = 7;
    public static final int SECOND_STAGE_MAX_AGE = 1;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, FIRST_STAGE_MAX_AGE + SECOND_STAGE_MAX_AGE);

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            FlaxCropBlock.box(0,0,0,16,2,16),
            FlaxCropBlock.box(0,0,0,16,4,16),
            FlaxCropBlock.box(0,0,0,16,6,16),
            FlaxCropBlock.box(0,0,0,16,8,16),
            FlaxCropBlock.box(0,0,0,16,10,16),
            FlaxCropBlock.box(0,0,0,16,12,16),
            FlaxCropBlock.box(0,0,0,16,14,16),
            FlaxCropBlock.box(0,0,0,16,16,16),
            FlaxCropBlock.box(0,0,0,16,16,16),
    };


    public FlaxCropBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(getAgeProperty(), 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[this.getAge(state)];
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isAreaLoaded(pos,1)) return;
        if (level.getRawBrightness(pos,0) >= 9){
            int currentAge = this.getAge(state);

            if (currentAge < this.getMaxAge()) {
                float growthSpeed = getGrowthSpeed(this.getStateForAge(currentAge),level,pos);

                if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int)((25/growthSpeed) + 1)) == 0)){
                    if (currentAge == FIRST_STAGE_MAX_AGE){
                        if (level.getBlockState(pos.above()).is(Blocks.AIR)){
                            level.setBlock(pos.above(), this.getStateForAge(currentAge + 1), 2);
                        }
                    }else {
                        level.setBlock(pos,this.getStateForAge(currentAge + 1), 2);
                    }
                    CommonHooks.fireCropGrowPost(level, pos, state);
                };
            }
        }
    }



    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState stateBelow = level.getBlockState(pos.below());

        if (state.getValue(AGE) == 8) {
            return stateBelow.is(this) && stateBelow.getValue(AGE) == 7;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int nextAge = this.getAge(state) + this.getBonemealAgeIncrease(level);
        int maxAge = this.getMaxAge();
        int age = this.getAge(state);
        if (nextAge > maxAge) {
            nextAge = maxAge;
        }

        if (age == FIRST_STAGE_MAX_AGE && level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos.above(), this.getStateForAge(nextAge), 2);
        }else {
            level.setBlock(pos, this.getStateForAge(nextAge - SECOND_STAGE_MAX_AGE), 2);
        }
    }

    @Override
    public int getMaxAge() {
        return FIRST_STAGE_MAX_AGE + SECOND_STAGE_MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.FLAX_SEEDS.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
