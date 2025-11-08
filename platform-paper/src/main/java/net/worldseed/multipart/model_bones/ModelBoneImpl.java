package net.worldseed.multipart.model_bones;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.worldseed.WorldSeedEntityEngine;
import net.worldseed.multipart.*;
import net.worldseed.multipart.animations.BoneAnimation;
import net.worldseed.utils.Point;
import net.worldseed.utils.Pos;
import net.worldseed.utils.Vec;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class ModelBoneImpl implements ModelBone {
    protected static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.class, EntityDataSerializers.INT
    );
    protected static final EntityDataAccessor<Integer> DATA_POS_ROT_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.class, EntityDataSerializers.INT
    );
    protected static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.class, EntityDataSerializers.INT
    );
    protected static final EntityDataAccessor<Vector3f> DATA_TRANSLATION_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.VECTOR3);
    protected static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.ItemDisplay.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(
            net.minecraft.world.entity.Display.ItemDisplay.class, EntityDataSerializers.ITEM_STACK
    );
    protected static final EntityDataAccessor<Quaternionf> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.QUATERNION);
    protected static final EntityDataAccessor<Quaternionf> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.QUATERNION);
    protected static final EntityDataAccessor<Vector3f> DATA_SCALE_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.VECTOR3);
    protected static final EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE_ID = SynchedEntityData.defineId(net.minecraft.world.entity.Display.class, EntityDataSerializers.INT);

    public enum ItemDisplayContext {
        NONE((byte) 0),
        THIRD_PERSON_LEFT_HAND((byte) 1),
        THIRD_PERSON_RIGHT_HAND((byte) 2),
        FIRST_PERSON_LEFT_HAND((byte) 3),
        FIRST_PERSON_RIGHT_HAND((byte) 4),
        HEAD((byte) 5),
        GUI((byte) 6),
        GROUND((byte) 7),
        FIXED((byte) 8),
        ON_SHELF((byte) 9);

        private final byte id;

        ItemDisplayContext(byte id) {
            this.id = id;
        }

        public byte getId() {
            return id;
        }
    }

    protected final WorldSeedEntityEngine plugin;
    protected final Map<String, ItemStack> items;
    protected final Point pivot;
    protected final String name;
    protected final List<BoneAnimation> allAnimations = new ArrayList<>();
    protected final ArrayList<ModelBone> children = new ArrayList<>();
    protected final GenericModel model;
    protected Point diff;
    protected float scale;
    protected Point offset;
    protected Point rotation;
    protected BoneEntity stand;
    private ModelBone parent;

    public ModelBoneImpl(WorldSeedEntityEngine plugin, Point pivot, String name, Point rotation, GenericModel model, float scale) {
        this.plugin = plugin;
        this.name = name;
        this.rotation = rotation;
        this.model = model;

        this.diff = model.getDiff(name);
        this.offset = model.getOffset(name);

        if (this.diff != null) this.pivot = pivot.add(this.diff);
        else this.pivot = pivot;

        this.items = ModelEngine.getItems(model.getId(), name);
        this.scale = scale;
    }

    @Override
    public BoneEntity getEntity() {
        return stand;
    }

    @Override
    public ModelBone getParent() {
        return parent;
    }

    @Override
    public void setParent(ModelBone parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setGlobalScale(float scale) {
        this.scale = scale;
    }

    public Point calculateGlobalRotation(Point endPos) {
        return calculateRotation(endPos, new Vec(0, 180 - model.getGlobalRotation(), 0), this.model.getPivot());
    }

    public Point calculateRotation(Point p, Point rotation, Point pivot) {
        Point position = p.sub(pivot);
        return ModelMath.rotate(position, rotation).add(pivot);
    }

    @Override
    public Point calculateScale(Point p, Point scale, Point pivot) {
        Point position = p.sub(pivot);
        return position.mul(scale).add(pivot);
    }

    public Point applyTransform(Point p) {
        Point endPos = p;

        if (this.diff != null) {
            endPos = calculateScale(endPos, this.getPropagatedScale(), this.pivot.sub(this.diff));
            endPos = calculateRotation(endPos, this.getPropagatedRotation(), this.pivot.sub(this.diff));
        } else {
            endPos = calculateScale(endPos, this.getPropagatedScale(), this.pivot);
            endPos = calculateRotation(endPos, this.getPropagatedRotation(), this.pivot);
        }

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == ModelLoader.AnimationType.TRANSLATION) {
                    var calculatedTransform = currentAnimation.getTransform();
                    endPos = endPos.add(calculatedTransform);
                }
            }
        }

        if (this.parent != null) {
            endPos = parent.applyTransform(endPos);
        }

        return endPos;
    }

    public Point getPropagatedRotation() {
        Point netTransform = Vec.ZERO;

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == ModelLoader.AnimationType.ROTATION) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.add(calculatedTransform);
                }
            }
        }

        return this.rotation.add(netTransform);
    }

    @Override
    public Point getPropagatedScale() {
        Point netTransform = Vec.ONE;

        for (BoneAnimation currentAnimation : this.allAnimations) {
            if (currentAnimation != null && currentAnimation.isPlaying()) {
                if (currentAnimation.getType() == ModelLoader.AnimationType.SCALE) {
                    Point calculatedTransform = currentAnimation.getTransform();
                    netTransform = netTransform.mul(calculatedTransform);
                }
            }
        }

        return netTransform;
    }

    @Override
    public Point calculateFinalScale(Point q) {
        if (this.parent != null) {
            Point pq = parent.calculateFinalScale(parent.getPropagatedScale());
            q = pq.mul(q);
        }

        return q;
    }

    public Quaternion calculateFinalAngle(Quaternion q) {
        if (this.parent != null) {
            Quaternion pq = parent.calculateFinalAngle(new Quaternion(parent.getPropagatedRotation()));
            q = pq.multiply(q);
        }

        return q;
    }

    public void addAnimation(BoneAnimation animation) {
        this.allAnimations.add(animation);
    }

    public void addChild(ModelBone child) {
        this.children.add(child);
    }

    @Override
    public void destroy() {
        this.children.forEach(ModelBone::destroy);
        this.children.clear();

        if (this.stand != null) {
            this.stand.discard();
        }
    }

    public CompletableFuture<Void> spawn(World world, Pos position) {
        if (this.offset != null && this.stand != null) {
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);
            ((CraftWorld) world).getHandle().addFreshEntity(this.stand);
            this.stand.setPos(position.x(), position.y(), position.z());
            this.stand.setRot(position.yaw(), position.pitch());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Point getOffset() {
        return this.offset;
    }

    public abstract Pos calculatePosition();

    public abstract Point calculateRotation();

    public abstract Point calculateScale();
}
