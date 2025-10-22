package net.worldseed.multipart.model_bones;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.worldseed.multipart.GenericModel;
import net.worldseed.utils.Pos;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class BoneEntity extends net.minecraft.world.entity.LivingEntity {
    private final GenericModel model;
    private final String name;

    public BoneEntity(@NotNull EntityType entityType, Level level, GenericModel model, String name) {
        super(entityType, level);
        this.setAutoViewable(false);
        setTag(Tag.String("WSEE"), "part");
        this.model = model;
        this.name = name;
        this.setCustomName(Component.literal(name));

        this.setNoGravity(true);
        this.setSynchronizationTicks(Integer.MAX_VALUE);
    }

    public @NotNull Set<Player> getViewers() {
        return model.getViewers();
    }

    public GenericModel getModel() {
        return model;
    }

    @Override
    public void tick() {
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        Pos position = this.getPosition();
        var spawnPacket = new SpawnEntityPacket(this.getEntityId(), this.getUuid(), this.getEntityType(), model.getPosition().withView(position.yaw(), 0), position.yaw(), 0, Vec.ZERO);

        player.sendPacket(spawnPacket);
        player.sendPacket(new LazyPacket(this::getMetadataPacket));

        if (this.getEntityType() == EntityType.ZOMBIE || this.getEntityType() == EntityType.ARMOR_STAND)
            player.sendPacket(getEquipmentsPacket());
    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }
}
