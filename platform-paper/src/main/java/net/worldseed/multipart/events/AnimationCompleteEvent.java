package net.worldseed.multipart.events;

import net.worldseed.multipart.GenericModel;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import org.jetbrains.annotations.NotNull;

public class AnimationCompleteEvent extends ModelEvent {
    private final GenericModel model;
    private final String animation;
    private final AnimationHandlerImpl.AnimationDirection direction;

    public AnimationCompleteEvent(@NotNull GenericModel model, String animation, AnimationHandlerImpl.AnimationDirection direction) {
        this.animation = animation;
        this.direction = direction;
        this.model = model;
    }
}
