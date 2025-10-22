package net.worldseed.multipart.mql;

import net.hollowcube.mql.jit.MqlEnv;
import net.worldseed.multipart.mql.MQLData;

public interface MQLEvaluator {
    double evaluate(@MqlEnv({"q", "query"}) MQLData data);
}
