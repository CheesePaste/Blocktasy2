package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.entity.BaseBlockEntity;
import cheesepaste.blocktasy.entity.FollowingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ControlableComponent extends EntityComponents{
    private UUID controllingPlayerId;

    private static final TrackedData<Boolean> IS_CONTROLLED =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // 操纵模式参数
    private static final float CONTROL_DISTANCE = 5.0f; // 操纵时的默认距离
    private static final float CONTROL_SPEED = 0.3f; // 操纵移动速度
    private static final float CONTROL_SMOOTHING = 0.2f; // 操纵平滑因子
    private static final TrackedData<Optional<UUID>> CONTROLLING_PLAYER_UUID =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    // 控制状态
    private Vec3d targetControlPos; // 控制目标位置
    private boolean justLaunched = false; // 刚刚被发射
    private int launchCooldown = 0; // 发射冷却
    
    ControlableComponent(BaseBlockEntity parent) {
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

    public void setControllingPlayer(@Nullable PlayerEntity player) {
        if (player != null) {
            this.controllingPlayerId = player.getUuid();
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.of(player.getUuid()));
            parent.getDataTracker().set(IS_CONTROLLED, true);

            // 启用发光效果
            parent.setGlowing(true);

            // 在操纵模式下，取消跟随目标
            if(parent.Components.get(TargetableComponent.class) instanceof TargetableComponent t){
                t.setTarget(null);
            };



            // 设置无重力，便于操纵
            parent.setNoGravity(true);

            // 初始位置：玩家面前一定距离
            Vec3d lookDirection = player.getRotationVec(1.0F);
            Vec3d eyePos = player.getEyePos();
            this.targetControlPos = eyePos.add(lookDirection.multiply(CONTROL_DISTANCE));

            // 平滑移动到目标位置
            Vec3d toTarget = targetControlPos.subtract(parent.getPos());
            double distance = toTarget.length();
            if (distance > 0.1) {
                Vec3d velocity = toTarget.normalize().multiply(CONTROL_SPEED);
                parent.setVelocity(velocity);
            } else {
                parent.setVelocity(Vec3d.ZERO);
            }

            // 清除发射状态
            justLaunched = false;
            launchCooldown = 0;

        } else {
            this.controllingPlayerId = null;
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.empty());
            parent.getDataTracker().set(IS_CONTROLLED, false);

            // 取消发光效果
            parent.setGlowing(false);

            // 恢复重力
            parent.setNoGravity(false);

            // 重置控制目标位置
            targetControlPos = parent.getPos();
        }
    }

    public boolean isControlledBy(PlayerEntity player) {
        if (player == null || this.controllingPlayerId == null) {
            return false;
        }
        return player.getUuid().equals(this.controllingPlayerId);
    }

    public boolean isControlled() {
        return parent.getDataTracker().get(IS_CONTROLLED);
    }

    @Nullable
    public PlayerEntity getControllingPlayer() {
        if (this.controllingPlayerId == null) {
            return null;
        }
        return parent.getWorld().getPlayerByUuid(this.controllingPlayerId);
    }
}
