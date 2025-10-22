package net.worldseed.multipart.model_bones.display_entity;

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.IntFunction;

public class RootBoneEntity extends BoneEntity {
    private static final net.minecraft.world.entity.Entity EMPTY_PLACEHOLDER;
    private static final Field VEHICLE_ENTITY_ID;
    private static final Field VEHICLE_PASSENGERS_ID;

    static {
        EMPTY_PLACEHOLDER = new net.minecraft.world.entity.Entity(EntityType.MINECART, null) {
            @Override
            protected void defineSynchedData(SynchedEntityData.Builder builder) {}

            @Override
            public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
                return false;
            }

            @Override
            protected void readAdditionalSaveData(ValueInput valueInput) {}

            @Override
            protected void addAdditionalSaveData(ValueOutput valueOutput) {}
        };
        try {
            VEHICLE_ENTITY_ID = net.minecraft.network.protocol.game.ClientboundSetPassengersPacket.class.getDeclaredField("vehicle");
            VEHICLE_PASSENGERS_ID = net.minecraft.network.protocol.game.ClientboundSetPassengersPacket.class.getDeclaredField("passengers");
            boolean accessible = VEHICLE_ENTITY_ID.trySetAccessible() && VEHICLE_PASSENGERS_ID.trySetAccessible();
            assert accessible : "net.minecraft.network.protocol.game.ClientboundSetPassengersPacket fields are not accessible";
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public RootBoneEntity(GenericModel model) {
        super(EntityType.ARMOR_STAND, ((CraftWorld) model.getWorld()).getHandle(), model, "root");
        this.setMarker(true);
        this.setInvisible(true);
        this.setNoGravity(true);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);

        List<Integer> parts = this.getModel().getParts().stream()
                .map(ModelBone::getEntity)
                .filter(e -> e != null && e.getType() == EntityType.ITEM_DISPLAY)
                .map(Entity::getId)
                .toList();


        net.minecraft.network.protocol.game.ClientboundSetPassengersPacket packet
                = new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(EMPTY_PLACEHOLDER);
        try {
            VEHICLE_ENTITY_ID.set(packet, this.getId());
            List<Entity> passengerEntities = this.getPassengers();
            int[] passengersId = new int[passengerEntities.size()];
            for (int i = 0; i < passengersId.length; i++) {
                passengersId[i] = passengerEntities.get(i).getId();
            }
            VEHICLE_PASSENGERS_ID.set(packet, passengersId);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public void setMarker(boolean marker) {
        this.entityData.set(net.minecraft.world.entity.decoration.ArmorStand.DATA_CLIENT_FLAGS,
                this.setBit(this.entityData.get(net.minecraft.world.entity.decoration.ArmorStand.DATA_CLIENT_FLAGS),
                        16, marker));
    }

    public boolean isMarker() {
        return (this.entityData.get(net.minecraft.world.entity.decoration.ArmorStand.DATA_CLIENT_FLAGS) & 16) != 0;
    }

    private byte setBit(byte oldBit, int offset, boolean value) {
        if (value) {
            oldBit = (byte)(oldBit | offset);
        } else {
            oldBit = (byte)(oldBit & ~offset);
        }

        return oldBit;
    }
}
