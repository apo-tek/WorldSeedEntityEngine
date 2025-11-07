package net.worldseed.multipart.model_bones.misc;

import net.kyori.adventure.util.RGBLike;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.worldseed.WorldSeedEntityEngine;
import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.Quaternion;
import net.worldseed.multipart.model_bones.BoneEntity;
import net.worldseed.multipart.model_bones.ModelBone;
import net.worldseed.multipart.model_bones.ModelBoneImpl;
import net.worldseed.multipart.model_bones.bone_types.RideableBone;
import net.worldseed.utils.Point;
import net.worldseed.utils.Pos;
import net.worldseed.utils.Vec;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModelBoneSeat extends ModelBoneImpl implements RideableBone {

    public ModelBoneSeat(WorldSeedEntityEngine plugin, World world, Point pivot, String name, Point rotation, GenericModel model, float scale) {
        super(plugin, pivot, name, rotation, model, scale);

        if (this.offset != null) {
            this.stand = new BoneEntity(EntityType.ARMOR_STAND, ((CraftWorld) world).getHandle(), model, name);
            this.stand.editEntityMeta(ArmorStandMeta.class, meta ->
                    meta.setMarker(true)
            );

            this.stand.setTag(Tag.String("WSEE"), "seat");
            stand.setInvisible(true);
        }
    }

    @Override
    public void addViewer(Player player) {
        if (this.stand != null) this.stand.addViewer(player);
    }

    @Override
    public void removeViewer(Player player) {
        if (this.stand != null) this.stand.removeViewer(player);
    }

    @Override
    public void removeGlowing() {

    }

    @Override
    public void setGlowing(RGBLike color) {

    }

    @Override
    public void removeGlowing(Player player) {

    }

    @Override
    public void setGlowing(Player player, RGBLike color) {

    }

    @Override
    public void attachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot attach a model to a seat");
    }

    @Override
    public List<GenericModel> getAttachedModels() {
        return List.of();
    }

    @Override
    public void detachModel(GenericModel model) {
        throw new UnsupportedOperationException("Cannot detach a model from a seat");
    }

    @Override
    public void setGlobalRotation(double yaw, double pitch) {

    }

    @Override
    public void setState(String state) {
    }

    @Override
    public Point getPosition() {
        return calculatePosition();
    }

    public CompletableFuture<Void> spawn(World world, Point position) {
        if (this.offset != null) {
            this.stand.setInvisible(true);
            this.stand.setNoGravity(true);
            this.stand.setSilent(true);
            ((CraftWorld) world).getHandle().addFreshEntity(this.stand);
            this.stand.setPos(position.x(), position.y(), position.z());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Pos calculatePosition() {
        if (this.offset == null) return Pos.ZERO;

        var rotation = calculateRotation();

        var p = applyTransform(this.offset);
        p = calculateGlobalRotation(p);
        Pos endPos = new Pos(p);

        return endPos
                .div(4, 4, 4).mul(scale)
                .add(model.getPosition())
                .add(model.getGlobalOffset())
                .withView((float) -rotation.y(), (float) rotation.x());
    }

    @Override
    public Point calculateRotation() {
        Quaternion q = new Quaternion(new Vec(0, 180 - this.model.getGlobalRotation(), 0));
        return q.toEulerYZX();
    }

    @Override
    public Point calculateScale() {
        return Vec.ZERO;
    }

    public void draw() {
        this.children.forEach(ModelBone::draw);
        if (this.offset == null) return;

        Pos found = calculatePosition();

        stand.setPos(found.x(), found.y(), found.z());
        stand.setRot(found.yaw(), found.pitch());
    }

    @Override
    public void addPassenger(Entity entity) {
        ((CraftEntity) entity).getHandle().startRiding(this.stand);
    }

    @Override
    public void removePassenger(Entity entity) {
        ((CraftEntity) entity).getHandle().stopRiding();
    }

    @Override
    public Set<Entity> getPassengers() {
        return Set.copyOf(this.stand.getBukkitEntity().getPassengers());
    }
}
