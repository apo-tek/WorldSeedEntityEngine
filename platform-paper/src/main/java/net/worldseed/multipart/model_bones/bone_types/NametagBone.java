package net.worldseed.multipart.model_bones.bone_types;

import net.worldseed.multipart.model_bones.ModelBone;
import org.bukkit.entity.Entity;

public interface NametagBone extends ModelBone {
    void bind(Entity nametag);
    void unbind();
    Entity getNametag();
}