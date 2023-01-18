package me.voidxwalker.serversiderng;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class RNGHandler {
    private final Map<RNGTypes, Random> randomMap;
    private long startTime;
    /**
     * Initializes a new {@link RNGHandler} using the provided {@code Long}. Great care is advised when using this constructor.
     * <p>
     * If the same {@code Random} flow as provided in {@link RNGSession#RNGSession} isn't followed, the random number generation might not be backwards reproducible by verifiers.
     * @param seed the {@code Long} used as a seed for the {@link RNGHandler#randomMap}
     * @author Void_X_Walker
     */
    protected RNGHandler(long seed) {
        randomMap = new LinkedHashMap<>();
        Random randomMapRandom = new Random(seed);
        for (RNGTypes type: RNGTypes.values()) {
            randomMap.put(type,new Random(randomMapRandom.nextLong()));
        }

    }
    /**
     * Creates a new {@link RNGHandler} using the provided {@code runId} and the {@code GetRandom} token obtained from the {@code Verification-Server}.
     * This method should be called asynchronous due to the delay associated with the request.
     * @param runId the {@code Long} {@code runId} of the session the newly created {@link RNGHandler} should belong to.
     * @return a new {@link RNGHandler} or {@code null} if an {@link IOException} occurred when making the request
     * @see  RNGHandler#RNGHandler(long)
     * @see ServerSideRNG#getGetRandomToken(long)
     * @author Void_X_Walker
     */
    public static RNGHandler createRNGHandlerOrNull(long runId) {
        try {
            return new RNGHandler(new Random(ServerSideRNG.getGetRandomToken(runId).get("random").getAsLong()).nextLong());
        } catch (IOException | NullPointerException e) {
            ServerSideRNG.LOGGER.warn("Failed to create new RNGHandler: ");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the next random {@code Long} for the specified type of {@link RNGTypes}
     * @param type the {@link RNGTypes} to return the random for
     * @return a long generated and logged by the {@code Verification-Server}
     * @author Void_X_Walker
     */
    public long getRngValue(RNGTypes type) {
        if (outOfNormalTime()) {
            if (outOfExtraTime()) {
                ServerSideRNG.LOGGER.warn("RNGHandler called for type " + type.name() + " is in extra time!");
            }
            else{
                ServerSideRNG.LOGGER.warn("RNGHandler called for type " + type.name() + " outside time!");
            }
        }
        ServerSideRNG.LOGGER.info("[" + RNGSession.getInstance().runId + "] Getting Random for " + type);
        return randomMap.get(type).nextLong();
    }
    /**
     * Activates the {@link RNGHandler} by setting the {@link RNGHandler#startTime} to the current {@code System Time}
     * @author Void_X_Walker
     */
    public void activate() {
        startTime = System.nanoTime();
    }
    /**
     * Returns whether the {@link RNGHandler} has passed its standard {@link ServerSideRNGConfig#USE_TIME}
     * @return {@code true} if the current {@link System#nanoTime()} minus the {@link RNGHandler#startTime} is bigger than the {@link ServerSideRNGConfig#USE_TIME}
     * @author Void_X_Walker
     */
    public boolean outOfNormalTime() {
        return System.nanoTime() - startTime > ServerSideRNGConfig.USE_TIME;
    }
    /**
     * Returns whether the {@link RNGHandler} has passed its {@link ServerSideRNGConfig#EXTRA_TIME}
     * @return {@code true} if the current {@link System#nanoTime()} minus the {@link RNGHandler#startTime} is bigger than the {@link ServerSideRNGConfig#USE_TIME} + the {@link ServerSideRNGConfig#EXTRA_TIME}
     * @author Void_X_Walker
     */
    public boolean outOfExtraTime() {
        return System.nanoTime() - startTime < ServerSideRNGConfig.USE_TIME + ServerSideRNGConfig.EXTRA_TIME;
    }
    /**
     * The Random Generators that get replaced with Generators on the {@code Verification-Server}
     * @author Void_X_Walker
     */
    public enum RNGTypes{
        MOB_DROP,
        BLOCK_DROP,
        BARTER,
        ENDER_DRAGON_ROTATION,
        ENDER_DRAGON_LANDING_APPROACH,
        ENDER_DRAGON_TARGET_HEIGHT,
        WORLD_SEED,
        THUNDER,
        PROJECTILE,
        FISHING_RESULT,
        FISHING_TIME
    }
}



