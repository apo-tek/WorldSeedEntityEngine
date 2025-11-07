package net.worldseed.utils;

import org.bukkit.Location;
import org.bukkit.World;

public final class PaperConverter {
    private PaperConverter() {}

    public static Pos locationToPos(Location location) {
        return new Pos(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static Location posToLocation(World world, Pos pos) {
        return new Location(world, pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
    }

    public static Pos nmsVecToPos(net.minecraft.world.phys.Vec3 vec) {
        return new Pos(vec.x(), vec.y(), vec.z());
    }
}
