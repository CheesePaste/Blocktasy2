package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.entity.BaseBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class TargetableComponent extends EntityComponents{
    TargetableComponent(BaseBlockEntity parent) {
        super(parent);
    }

    public void setTarget(PlayerEntity target) {
    }
}
