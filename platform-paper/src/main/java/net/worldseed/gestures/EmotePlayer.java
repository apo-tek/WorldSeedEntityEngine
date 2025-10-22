package net.worldseed.gestures;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.worldseed.WorldSeedEntityEngine;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.multipart.events.ModelDamageEvent;
import net.worldseed.multipart.events.ModelInteractEvent;
import net.worldseed.utils.PlayerSkin;
import net.worldseed.utils.Pos;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class EmotePlayer extends Monster implements Listener {
    private static final Field DIMENSIONS;

    static {
        try {
            DIMENSIONS = Entity.class.getDeclaredField("dimensions");
            boolean accessible = DIMENSIONS.trySetAccessible();
            assert accessible : "Could not access Entity.dimensions";
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private final EmoteModel model;
    private final AnimationHandler animationHandler;
    private int emoteIndex = 0;

    public EmotePlayer(WorldSeedEntityEngine plugin, MinecraftServer server, Level level, Pos pos, PlayerSkin skin, EntityType<? extends Monster> entityType) {
        super(entityType, level);

        Entity self = this;
        this.model = new EmoteModel(skin) {
            @Override
            public void setPosition(Pos pos) {
                super.setPosition(pos);
                if (self.getInstance() != null) self.teleport(pos);
            }
        };

        model.init(instance, pos);

        EntityDimensions dimensions = EntityDimensions.fixed(0.8f, 1.8f);
        try {
            DIMENSIONS.set(this, dimensions);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.setInvisible(true);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.001f);

        this.setInstance(instance, pos).join();

        this.animationHandler = new AnimationHandlerImpl(plugin, model) {
            @Override
            protected void loadDefaultAnimations() {
            }
        };

        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.model.draw();
        this.model.draw();
    }

    public EmotePlayer(Instance instance, Pos pos, PlayerSkin skin) {
        this(instance, pos, skin, EntityType.ZOMBIE);
    }

    @EventHandler
    public void dispatchModelDamageEvent(EntityDamageEvent entityDamageEvent) {
        if (!((CraftEntity) entityDamageEvent.getEntity()).getHandle().equals(this))
            return;

        entityDamageEvent.setCancelled(true);
        ModelDamageEvent modelDamageEvent = new ModelDamageEvent();
        Bukkit.getPluginManager().callEvent(modelDamageEvent);
    }

    @EventHandler
    public void dispatchModelInteractEvent(PlayerInteractEntityEvent playerInteractEntityEvent) {
        if (!((CraftEntity) playerInteractEntityEvent.getRightClicked()).getHandle().equals(this))
            return;

        playerInteractEntityEvent.setCancelled(true);
        ModelInteractEvent modelInteractEvent = new ModelInteractEvent();
        Bukkit.getPluginManager().callEvent(modelInteractEvent);
    }

    /**
     * Loads the emotes into the animation handler
     *
     * @param emotes Map containing the emote name, and emote data
     */
    public void loadEmotes(Map<String, JsonObject> emotes) {
        for (Map.Entry<String, JsonObject> entry : emotes.entrySet()) {
            this.animationHandler.registerAnimation(entry.getKey(), entry.getValue(), emoteIndex);
            emoteIndex++;
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        this.model.destroy();
        this.animationHandler.destroy();
        super.onRemoval(reason);
    }

    @Override
    public void tick() {
        var oldPosition = new Pos(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        super.tick();
        var currentPosition = new Pos(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        if (oldPosition.equals(currentPosition)) return;
        this.model.setPosition(currentPosition);
    }

    public void setRotation(float yaw) {
        this.model.setGlobalRotation(yaw);
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer != null)
            this.model.addViewer((Player) player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer != null)
            this.model.removeViewer(bukkitPlayer);
    }

    protected AnimationHandler getAnimationHandler() {
        return animationHandler;
    }
}
