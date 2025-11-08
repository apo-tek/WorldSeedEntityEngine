package net.worldseed.multipart.model_bones.display_entity;

import net.kyori.adventure.util.RGBLike;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.worldseed.WorldSeedEntityEngine;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.ModelBoneViewable;
import net.worldseed.utils.PaperConverter;
import net.worldseed.utils.Point;
import net.worldseed.utils.Pos;
import net.worldseed.utils.Vec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModelBonePartDisplay extends ModelBoneImpl implements ModelBoneViewable {
    private final List<GenericModel> attached = new ArrayList<>();
    private Entity baseStand;

    public ModelBonePartDisplay(WorldSeedEntityEngine plugin, World world, Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(plugin, pivot, name, rotation, model, scale);

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ITEM_DISPLAY, ((CraftWorld) world).getHandle(), model, name);
            this.stand.getEntityData().set(DATA_SCALE_ID, new Vector3f(scale, scale, scale));
            this.stand.getEntityData().set(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.FIXED.getId());
            this.stand.getEntityData().set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 2);
            this.stand.getEntityData().set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 2);
            this.stand.getEntityData().set(DATA_VIEW_RANGE_ID, 1000f);
        }
    }

    @Override
    public void addViewer(Player player) {
        if (player != null) {
            player.showEntity(this.plugin, this.stand.getBukkitEntity());
            player.showEntity(this.plugin, this.baseStand);
        }
        this.attached.forEach(model -> model.addViewer(player));
    }

    @Override
    public void removeGlowing() {
        if (this.stand != null) {
            this.stand.getBukkitEntity().setGlowing(false);
        }

        this.attached.forEach(GenericModel::removeGlowing);
    }

    @Override
    public void setGlowing(RGBLike color) {
        if (this.stand != null) {
            int rgb = 0;
            rgb |= color.red() << 16;
            rgb |= color.green() << 8;
            rgb |= color.blue();

            this.stand.getEntityData().set(DATA_GLOW_COLOR_OVERRIDE_ID, rgb);
            this.stand.getBukkitEntity().setGlowing(true);
        }

        this.attached.forEach(model -> model.setGlowing(color));
    }

    @Override
    public void removeGlowing(Player player) {
        if (this.stand == null)
            return;

        int id = this.stand.getId();
        this.stand.getBukkitEntity().setGlowing(false);
        this.stand.getEntityData().set(DATA_GLOW_COLOR_OVERRIDE_ID, -1);
        net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(id, this.stand.getEntityData().packAll());
        ((CraftPlayer) player).getHandle().connection.send(dataPacket);
        this.attached.forEach(model -> model.removeGlowing(player));
    }

    @Override
    public void setGlowing(Player player, RGBLike color) {
        if (this.stand == null)
            return;

        int rgb = 0;
        rgb |= color.red() << 16;
        rgb |= color.green() << 8;
        rgb |= color.blue();

        int id = this.stand.getId();
        this.stand.getBukkitEntity().setGlowing(true);
        this.stand.getEntityData().set(DATA_GLOW_COLOR_OVERRIDE_ID, rgb);
        net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(id, this.stand.getEntityData().packAll());
        ((CraftPlayer) player).getHandle().connection.send(dataPacket);
        this.attached.forEach(model -> model.removeGlowing(player));
        this.attached.forEach(model -> model.setGlowing(player, color));
    }

    @Override
    public void attachModel(GenericModel model) {
        attached.add(model);
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return attached;
    }

    @Override
    public void detachModel(GenericModel model) {
        attached.remove(model);
    }

    @Override
    public void setGlobalRotation(double yaw, double pitch) {
        if (this.stand != null) {
            var correctYaw = (180 + yaw + 360) % 360;
            var correctPitch = (pitch + 360) % 360;
            this.stand.setRot((float) correctYaw, (float) correctPitch);
        }
    }

    @Override
    public void removeViewer(Player player) {
        if (player != null) {
            player.hideEntity(this.plugin, this.stand.getBukkitEntity());
            player.hideEntity(this.plugin, this.baseStand);
        }
        this.attached.forEach(model -> model.removeViewer(player));
    }

    @Override
    public void destroy() {
        super.destroy();
        if (this.baseStand != null) {
            this.baseStand.remove();
        }
    }

    @Override
    public Pos calculatePosition() {
        return new Pos(model.getPosition()).withView(0, 0);
    }

    private Pos calculatePositionInternal() {
        if (this.offset == null) return Pos.ZERO;
        Point p = this.offset;
        p = applyTransform(p);
        return new Pos(p).div(4).mul(scale).withView(0, 0);
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = calculateFinalAngle(new Quaternion(getPropagatedRotation()));
        return q.toEuler();
    }

    @Override
    public Point calculateScale() {
        return calculateFinalScale(getPropagatedScale());
    }

    @Override
    public void teleport(Point position) {
        if (this.baseStand != null) this.baseStand.teleport(new Location(this.baseStand.getWorld(), position.x(), position.y(), position.z()));
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        if (this.stand != null) {
            var position = calculatePositionInternal();
            var scale = calculateScale();

            Quaternion q = calculateFinalAngle(new Quaternion(getPropagatedRotation()));
            this.stand.getEntityData().set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
            this.stand.getEntityData().set(DATA_SCALE_ID, new Vector3f((float) (scale.x() * this.scale), (float) (scale.y() * this.scale), (float) (scale.z() * this.scale)));
            this.stand.getEntityData().set(DATA_RIGHT_ROTATION_ID, new Quaternionf(q.x(), q.y(), q.z(), q.w()));
            this.stand.getEntityData().set(DATA_TRANSLATION_ID, new Vector3f((float) position.x(), (float) position.y(), (float) position.z()));
            attached.forEach(model -> {
                model.setPosition(this.model.getPosition().add(calculateGlobalRotation(position)));
                model.setGlobalRotation(-q.toEuler().x() + this.model.getGlobalRotation());
                model.draw();
            });
        }
    }

    @Override
    public CompletableFuture<Void> spawn(World world, Pos position) {
        var correctLocation = (180 + this.model.getGlobalRotation() + 360) % 360;
        return super.spawn(world, new Pos(position).withYaw((float) correctLocation)).whenCompleteAsync((_, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }

            if (!(this.getParent() instanceof ModelBonePartDisplay)) {
                BoneEntity baseBone = model.generateRoot();
                assert baseBone != null;
                this.baseStand = baseBone.getBukkitEntity();
                ((CraftWorld) world).getHandle().addFreshEntity(baseBone);
                this.baseStand.teleport(PaperConverter.posToLocation(world, position));
            }
        });
    }

    @Override
    public void setState(String state) {
        if (this.stand != null && this.stand.getType() == EntityType.ITEM_DISPLAY) {
            if (state.equals("invisible")) {
                this.stand.getEntityData().set(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
                return;
            }

            var item = this.items.get(state);
            if (item != null) {
                this.stand.getEntityData().set(DATA_ITEM_STACK_ID, CraftItemStack.asNMSCopy(item));
            }
        }
    }

    @Override
    public Point getPosition() {
        return calculatePositionInternal().add(model.getPosition());
    }
}
