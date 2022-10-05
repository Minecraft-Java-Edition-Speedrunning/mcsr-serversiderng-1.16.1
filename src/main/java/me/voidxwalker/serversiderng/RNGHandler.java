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
        this.randomMap.put(RNGHandler.RNGTypes.MOB_DROP,new Random(random.nextLong()));
        this.randomMap.put(RNGHandler.RNGTypes.BLOCK_DROP,new Random(random.nextLong()));
        this.randomMap.put(RNGHandler.RNGTypes.BARTER,new Random(random.nextLong()));
        this.randomMap.put(RNGHandler.RNGTypes.WORLD_SEED,new Random(random.nextLong()));
        this.randomMap.put(RNGHandler.RNGTypes.ENDER_DRAGON_ROTATION,new Random(random.nextLong()));
        this.randomMap.put(RNGHandler.RNGTypes.ENDER_DRAGON_LANDING_APPROACH,new Random(random.nextLong()));
    }
    /**
     * Returns the next random {@code Long} for the specified type of {@code RNGTypes}
     * @author Void_X_Walker
     */
    public long getRngValue(RNGTypes type) {
        if(this.outOfNormalTime() ){
            if(this.outOfExtraTime()){
                ServerSideRng.LOGGER.warn("RNGHandler called for type "+type.name()+" is in extra time!");
            }
            else{
                ServerSideRng.LOGGER.warn("RNGHandler called for type "+type.name()+" outside time!");
            }
        }
        ServerSideRng.LOGGER.info("Getting Random for "+type);
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
        WORLD_SEED
    }
}



