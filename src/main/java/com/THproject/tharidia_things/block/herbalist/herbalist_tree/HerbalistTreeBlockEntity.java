package com.THproject.tharidia_things.block.herbalist.herbalist_tree;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.herbalist.pot.PotBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HerbalistTreeBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation GROW_ANIM = RawAnimation.begin().thenPlayAndHold("grow");
    private static final RawAnimation ROOT1_ANIM = RawAnimation.begin().thenPlayAndHold("root");
    private static final RawAnimation ROOT2_ANIM = RawAnimation.begin().thenPlayAndHold("root2");
    private static final RawAnimation ROOT3_ANIM = RawAnimation.begin().thenPlayAndHold("root3");
    private static final RawAnimation ROOT4_ANIM = RawAnimation.begin().thenPlayAndHold("root4");

    public HerbalistTreeBlockEntity(BlockPos pos, BlockState state) {
        super(TharidiaThings.HERBALIST_TREE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    public boolean hasPotAtRoot(int rootIndex) {
        Level level = this.getLevel();
        if (level == null) return false;
        BlockPos potPos = getPotPositionForRoot(rootIndex);
        return level.getBlockState(potPos).getBlock() instanceof PotBlock;
    }

    public BlockPos getPotPositionForRoot(int rootIndex) {
        BlockPos pos = this.getBlockPos();
        Direction forwardDir = Direction.NORTH;

        for (int i = 1; i <= rootIndex; i++) {
            if (i == rootIndex) {
                BlockPos newPos = pos.relative(forwardDir, 2);
                Direction sideDir = (i % 2 != 0) ? forwardDir.getCounterClockWise() : forwardDir.getClockWise();
                return newPos.relative(sideDir, 1);
            }
            if (i % 2 == 0) {
                forwardDir = forwardDir.getClockWise();
            }
        }
        return pos;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Tree growth animation - plays once on placement and holds at final frame
        controllers.add(new AnimationController<>(this, "grow", 0, state -> {
            if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                state.getController().setAnimation(GROW_ANIM);
            }
            return PlayState.CONTINUE;
        }));

        // Branch animation controllers for roots that have animations
        addRootController(controllers, 1, ROOT1_ANIM);  // Radice1 -> "root"
        addRootController(controllers, 3, ROOT3_ANIM);  // Radice3 -> "root3"
        addRootController(controllers, 4, ROOT4_ANIM);  // Radice4 -> "root4"
        addRootController(controllers, 8, ROOT2_ANIM);  // Radice8 -> "root2"
    }

    private void addRootController(ControllerRegistrar controllers, int rootIndex, RawAnimation anim) {
        controllers.add(new AnimationController<>(this, "root" + rootIndex + "_ctrl", 0, state -> {
            if (hasPotAtRoot(rootIndex)) {
                state.getController().setAnimation(anim);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
