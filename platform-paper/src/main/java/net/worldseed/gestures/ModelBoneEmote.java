package net.worldseed.gestures;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.kyori.adventure.util.RGBLike;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.worldseed.WorldSeedEntityEngine;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;
import net.worldseed.utils.PlayerSkin;
import net.worldseed.utils.Point;
import net.worldseed.utils.Pos;
import net.worldseed.utils.Vec;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModelBoneEmote extends ModelBoneImpl implements ModelBoneViewable {
    private static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Vector3f> DATA_TRANSLATION_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.ItemDisplay.class, EntityDataSerializers.BYTE);
    private static final byte THIRD_PERSON_RIGHT_HAND = 2;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.ItemDisplay.class, EntityDataSerializers.ITEM_STACK
    );
    private static final EntityDataAccessor<Quaternionf> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Quaternionf> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Vector3f> DATA_SCALE_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.VECTOR3);


    private final WorldSeedEntityEngine plugin;
    private final Double verticalOffset;

    public ModelBoneEmote(WorldSeedEntityEngine plugin, World world, Point pivot, String name, Point rotation, GenericModel model, int translation, Double verticalOffset, PlayerSkin skin) {
        super(plugin, pivot, name, rotation, model, 1);

        this.plugin = plugin;
        this.verticalOffset = verticalOffset;

        if (this.offset != null) {
            this.stand = new BoneEntity(net.minecraft.world.entity.EntityType.ITEM_DISPLAY, ((CraftWorld) world).getHandle(), model, name);
            this.stand.getEntityData().set(DATA_VIEW_RANGE_ID, 10000f);
            this.stand.getEntityData().set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 2);
            this.stand.getEntityData().set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 2);
            this.stand.getEntityData().set(DATA_TRANSLATION_ID, new Vector3f(0, translation, 0));
            this.stand.getEntityData().set(DATA_ITEM_DISPLAY_ID, THIRD_PERSON_RIGHT_HAND);
            net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(Items.PLAYER_HEAD);
            PropertyMap propertyMap = new PropertyMap();
            propertyMap.put("textures", new Property("textures", skin.textures(), skin.signature()));
            itemStack.set(DataComponents.PROFILE, new ResolvableProfile(Optional.empty(), Optional.empty(), propertyMap));
            itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(customModelDataFromName(name)), List.of(), List.of(), List.of()));
            this.stand.getEntityData().set(DATA_ITEM_STACK_ID, itemStack);
        }

        switch (this.name) {
            case "Head", "Body" -> this.diff = this.pivot.add(0, 0, 0);
            case "RightArm" -> this.diff = this.pivot.add(-1.17, 0, 0);
            case "LeftArm" -> this.diff = this.pivot.add(1.17, 0, 0);
            case "RightLeg" -> this.diff = this.pivot.add(-0.4446, 0, 0);
            case "LeftLeg" -> this.diff = this.pivot.add(0.4446, 0, 0);
        }
    }

    @Override
    public CompletableFuture<Void> spawn(World world, Pos position) {
        var correctLocation = (180 + this.model.getGlobalRotation() + 360) % 360;
        return super.spawn(world, new Pos(position).withYaw((float) correctLocation)).whenCompleteAsync((_, e) -> {
            if (e != null) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (this.stand != null) {
            var scale = calculateScale();
            var position = calculatePosition();

            if (this.stand.getType() == EntityType.ITEM_DISPLAY) {
                Quaternion q = new Quaternion(calculateRotation());
                this.stand.getEntityData().set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
                this.stand.getEntityData().set(DATA_SCALE_ID,
                        new Vector3f((float) (scale.x() * this.scale), (float) (scale.y() * this.scale), (float) (scale.z() * this.scale)));
                this.stand.getEntityData().set(DATA_RIGHT_ROTATION_ID, new Quaternionf(q.x(), q.y(), q.z(), q.w()));
                this.stand.setPos(position.x(), position.y(), position.z());
                this.stand.setRot(0, 0);
            }
        }
    }

    @Override
    public Pos calculatePosition() {
        Point p = this.offset == null ? Pos.ZERO : this.offset;
        p = applyTransform(p);
        p = calculateGlobalRotation(p);

        return new Pos(p)
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition())
                .add(0, verticalOffset, 0)
                .add(model.getGlobalOffset());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropagatedRotation()));
        Quaternion pq = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        q = pq.multiply(q);

        return q.toEuler();
    }

    @Override
    public Point calculateScale() {
        return Vec.ONE;
    }

    private float customModelDataFromName(String name) {
        return switch (name) {
            case "Head" -> 1;
            case "RightArm" -> 2;
            case "LeftArm" -> 3;
            case "Body" -> 4;
            case "RightLeg" -> 5;
            case "LeftLeg" -> 6;
            case "slim_right" -> 7;
            case "slim_left" -> 8;
            default -> 0;
        };
    }

    @Override
    public void setState(String state) {
        throw new UnsupportedOperationException("Cannot set state on an emote");
    }

    @Override
    public Point getPosition() {
        return calculatePosition();
    }

    @Override
    public void addViewer(@NotNull Player player) {
        if (this.stand != null) {
            player.showEntity(this.plugin, this.stand.getBukkitEntity());
        }
    }

    @Override
    public void removeViewer(@NotNull Player player) {
        if (this.stand != null) {
            player.hideEntity(this.plugin, this.stand.getBukkitEntity());
        }
    }

    @Override
    public void removeGlowing() {
        if (this.stand != null) {
            this.stand.getBukkitEntity().setGlowing(false);
        }
    }

    @Override
    public void setGlowing(RGBLike color) {
        if (this.stand != null) {
            this.stand.getBukkitEntity().setGlowing(true);
        }
    }

    @Override
    public void removeGlowing(Player player) {

    }

    @Override
    public void setGlowing(Player player, RGBLike color) {

    }

    @Override
    public void attachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot attach a model to this bone type");
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return List.of();
    }

    @Override
    public void detachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot detach a model from this bone type");
    }

    @Override
    public void setGlobalRotation(double yaw, double pitch) {
    }
}