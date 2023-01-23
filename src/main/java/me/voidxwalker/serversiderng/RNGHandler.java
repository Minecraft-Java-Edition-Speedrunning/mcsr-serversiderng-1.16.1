package me.voidxwalker.serversiderng;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class RNGHandler {
    private final Map<RNGTypes, RNGSupplier> randomMap;
    private long startTime;
    private int handlerIndex;
    /**
     * Initializes a new {@link RNGHandler} using the provided {@code Long}. Great care is advised when using this constructor.
     * <p>
     * If the same {@code Random} flow as provided in {@link RNGSession#RNGSession} isn't followed, the random number generation might not be backwards reproducible by verifiers.
     * @param seed the {@code Long} used as a seed for the {@link RNGHandler#randomMap}
     * @author Void_X_Walker
     */
    protected RNGHandler(long seed) {
        randomMap = new LinkedHashMap<>();
        fillRandomMap(new Random(seed));
        handlerIndex=-1;

    }
    private void fillRandomMap(Random randomMapRandom){
        for (RNGTypes type: RNGTypes.values()) {
            randomMap.put(type,new RNGSupplier(randomMapRandom.nextLong(),type));
        }
    }
    public void log(Level level,String message){
        RNGSession.getInstance().log(level,"("+this.handlerIndex+")"+message);
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
            ServerSideRNG.log(Level.WARN,"Failed to create new RNGHandler: ");
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
    protected long getRngValue(RNGTypes type,@Nullable String subType) {
        if (outOfNormalTime()) {
            if (outOfExtraTime()) {
                this.log(Level.WARN,"RNGHandler called for type " + type.name() + " is in extra time!");
            }
            else{
                this.log(Level.WARN,"RNGHandler called for type " + type.name() + " outside time!");
            }
        }
        this.log(Level.INFO,"Getting Random for " + type + (subType==null?"":"| "+subType)+ " ("+randomMap.get(type).getUseCases(subType)+")");
        return randomMap.get(type).getRandom(subType).nextLong();
    }
    /**
     * Activates the {@link RNGHandler} by setting the {@link RNGHandler#startTime} to the current {@code System Time}
     * @author Void_X_Walker
     */
    public void activate(int handlerIndex) {
        startTime = System.nanoTime();
        this.handlerIndex=handlerIndex;
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
        MOB_DROP(true){
            @Override
            public void populateRandomMap(Map<String,RandomMapEntry> randomMap,Random random){
                DefaultedRegistry<EntityType<?>> registry=Registry.ENTITY_TYPE;
                for (EntityType<?> entityType : registry) {
                    randomMap.put(entityType.getTranslationKey(),new RandomMapEntry( new Random(random.nextLong())));
                }
            }
        },
        BLOCK_DROP(true){
            @Override
            public void populateRandomMap(Map<String,RandomMapEntry> randomMap,Random random){
                DefaultedRegistry<Block> registry=Registry.BLOCK;
                for (Block block : registry) {
                    randomMap.put(block.getTranslationKey(),new RandomMapEntry( new Random(random.nextLong())));
                }
            }
        },
        BARTER,
        ENDER_DRAGON_ROTATION,
        ENDER_DRAGON_LANDING_APPROACH,
        ENDER_DRAGON_TARGET_HEIGHT,
        ENDER_DRAGON_PATH,
        WORLD_SEED,
        THUNDER,
        PROJECTILE,
        FISHING_RESULT,
        FISHING_TIME,
        VILLAGER_SELECT_OFFER(true){
            @Override
            public void populateRandomMap(Map<String, RandomMapEntry> randomMap, Random random){
                DefaultedRegistry<VillagerProfession> registry=Registry.VILLAGER_PROFESSION;
                for (VillagerProfession profession : registry) {
                    randomMap.put(profession.toString(),new RandomMapEntry( new Random(random.nextLong())));
                }
            }
        },
        VILLAGER_OFFER(true){
            @Override
            public void populateRandomMap(Map<String, RandomMapEntry> randomMap, Random random){
                DefaultedRegistry<VillagerProfession> registry=Registry.VILLAGER_PROFESSION;
                for (VillagerProfession profession : registry) {
                    randomMap.put(profession.toString(),new RandomMapEntry(new Random(random.nextLong())));
                }
            }
        },
        ENCHANTMENT;
        public void populateRandomMap(Map<String, RandomMapEntry> randomMap, Random random){
        }
        private final boolean branchedRandom;
        public boolean usesBranchedRandom(){
            return branchedRandom;
        }
        RNGTypes(boolean branchedRandom){
            this.branchedRandom=branchedRandom;
        }
        RNGTypes(){
            this(false);
        }

    }
    public class RNGSupplier {
        private final LinkedHashMap<String,RandomMapEntry> randomMap;
        public Random getRandom(@Nullable String subType){
            RandomMapEntry entry;
            if(subType!=null&&randomMap.containsKey(subType)){
                entry= randomMap.get(subType);
            }
            else{
                entry= randomMap.get("startRandom");
            }
            entry.useTimes++;
            return entry.random;
        }
        public int getUseCases(String subType){
            RandomMapEntry entry;
            if(subType!=null&&randomMap.containsKey(subType)){
                entry= randomMap.get(subType);
            }
            else{
                entry= randomMap.get("startRandom");
            }
            return entry.useTimes;
        }
        public RNGSupplier(long seed,RNGTypes types){
            Random startRandom = new Random(seed);
            boolean branchedRandom = types.usesBranchedRandom();
            this.randomMap=new LinkedHashMap<>();
            if(branchedRandom){
                types.populateRandomMap(randomMap, startRandom);
            }
            randomMap.put("startRandom",new RandomMapEntry(startRandom));
        }

    }
    static class RandomMapEntry{
        public Random random;
        public int useTimes;
        public RandomMapEntry(Random random){
            this.random=random;
            useTimes=0;
        }
    }
}



