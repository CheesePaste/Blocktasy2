package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.entity.BaseBlockEntity;
import cheesepaste.blocktasy.entity.FollowingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;
import java.util.UUID;

public class TargetableComponent extends EntityComponents{
    private PlayerEntity target;
    private static final TrackedData<Optional<UUID>> TARGET_UUID =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    int targetLostCounter=0;
    TargetableComponent(BaseBlockEntity parent) {
        super(parent);
    }

    @Override
    public void tick() {

    }

    @Override
    public void initDT(DataTracker.Builder builder) {

    }

    @Override
    public void readNBT(NbtCompound nbt) {

    }

    @Override
    public void writeNBT(NbtCompound nbt) {

    }

    public void setTarget(PlayerEntity target) {
        Entity oldTarget = this.target;
        this.target = target;

        if (target != null) {
            parent.getDataTracker().set(TARGET_UUID, Optional.of(target.getUuid()));
            targetLostCounter = 0;
        } else {
            parent.getDataTracker().set(TARGET_UUID, Optional.empty());
        }
    }
}
