package me.voidxwalker.serversiderng;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class RNGHandler {
    static final long DEFAULT_USE_TIME=40000000000L; //40 seconds
    static final long DEFAULT_EXTRA_TIME=10000000000L; //10 seconds
    Map<RNGHandler.RNGTypes, Random> randomMap;
    long startTime;
    long useTime;
    long extraTime;
    public RNGHandler(long seed) {
        this.initRandomMap(new Random(seed));
        this.useTime=DEFAULT_USE_TIME;
        this.extraTime=DEFAULT_EXTRA_TIME;
    }
    void initRandomMap(Random random){
        this.randomMap =new LinkedHashMap<>();
        for(RNGTypes type:RNGTypes.values()){
            this.randomMap.put(type,new Random(random.nextLong()));
        }
    }
    /**
     * Returns the next random {@code Long} for the specified type of {@code RNGTypes}
     * @author Void_X_Walker
     */
    public long getRngValue(RNGTypes type) {
        if(this.outOfNormalTime() ){
            if(this.outOfExtraTime()){
                ServerSideRNG.LOGGER.warn("RNGHandler called for type "+type.name()+" is in extra time!");
            }
            else{
                ServerSideRNG.LOGGER.warn("RNGHandler called for type "+type.name()+" outside time!");
            }
        }
        ServerSideRNG.LOGGER.info("["+ Speedrun.currentSpeedrun.runId+"] Getting Random for "+type);
        return randomMap.get(type).nextLong();
    }
    public void activate(){
        startTime=System.nanoTime();
    }
    public boolean outOfNormalTime(){
        return System.nanoTime()-this.startTime>this.useTime;
    }
    public boolean outOfExtraTime(){
        return System.nanoTime()-startTime<this.useTime + this.extraTime;
    }
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



