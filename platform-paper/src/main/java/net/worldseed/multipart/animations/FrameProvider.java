package net.worldseed.multipart.animations;

import net.worldseed.utils.Point;
import net.worldseed.utils.Vec;

public interface FrameProvider {
    Point RotationMul = new Vec(-1, -1, 1);
    Point TranslationMul = new Vec(-1, 1, 1);

    Point getFrame(int tick);
}
