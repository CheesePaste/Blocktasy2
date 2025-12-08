package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.entity.BaseBlockEntity;
import net.minecraft.entity.Entity;

public abstract class  EntityComponents {
    protected BaseBlockEntity parent;

    EntityComponents(BaseBlockEntity parent){
        this.parent=parent;
    }
}
