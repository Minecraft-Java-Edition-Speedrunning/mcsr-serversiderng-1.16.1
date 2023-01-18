package me.voidxwalker.serversiderng;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ServerSideRNGConfig {
    public final static long TIME_OUT_OF_WORLD_BEFORE_AUTOUPLOAD=10000000000L;
    final static String BASE_URL = "https://serverside-rng-website-fputsekrmq-uc.a.run.app";
    final static String UPLOAD_HASH_URL = BASE_URL + "/uploadHash";
    final static String GET_RANDOM_URL = BASE_URL + "/getRandom";
    final static String START_RUN_URL = BASE_URL + "/startRun";
    final static String READ_ME_NAME = "readme.txt";
    final static String READ_ME = "Submit the Verification Zip File with the name the world you played your run in alongside your speedrun.com submission.\nMake sure not to alter the ZIP in any way, as that may lead your run becoming unverifiable.\nFor more information read this: https://github.com/VoidXWalker/serverSideRNG/blob/master/README.md.\nIf you have any problems or unanswered questions feel free to open a help thread in the Minecraft Java Edition Speedrunning Discord: https://discord.com/invite/jmdFn3C.";
    final static long USE_TIME = 40000000000L; //40 seconds
    final static long EXTRA_TIME = 10000000000L; //10 seconds

    final static long GRACE_PERIOD=3000000000L; // 3 seconds
    public static boolean UPLOAD_ON_SEED=true;
    public static boolean UPLOAD_ON_SHUTDOWN=true;
    public static boolean UPLOAD_ON_WORLD_LEAVE=true;

    static void init() {
        new File("config").mkdir();
        new File("config/serversiderng").mkdir();
        File configFile = new File("config/serversiderng/serversiderng.properties");
        try {
            if (configFile.createNewFile()) {
                storeDefaultProperties(configFile);
            }
            Properties properties = getProperties(configFile);
            UPLOAD_ON_SEED = Boolean.parseBoolean(properties.getProperty("uploadOnSeed", "true"));
            UPLOAD_ON_SHUTDOWN = Boolean.parseBoolean(properties.getProperty("uploadOnShutdown", "true"));
            UPLOAD_ON_WORLD_LEAVE = Boolean.parseBoolean(properties.getProperty("uploadOnWorldLeave", "true"));
        } catch (IOException e){
            ServerSideRNG.log(Level.WARN, "Could not load config:\n" + e.getMessage());
        }

    }
    static void storeDefaultProperties(File configFile) {
        try (FileWriter f = new FileWriter(configFile)) {
            Properties properties = new Properties();
            properties.put("uploadOnSeed",""+ true);
            properties.put("uploadOnShutdown",""+ true);
            properties.put("uploadOnWorldLeave", ""+true);
            properties.store(f, "This is the config file for ServerSideRNG.\nuploadOnSeed: Zip the world and upload its hash on the /seed command \nuploadOnShutdown Zip the world and upload its hash on when the game closes \nuploadOnWorldLeave Zip the world and upload its hash when not being in a world for 10 seconds");
        } catch (IOException e) {
            ServerSideRNG.log(Level.WARN, "Could not save config file:\n" + e.getMessage());
        }
    }
    static Properties getProperties(File configFile){
        try(FileInputStream f= new FileInputStream(configFile)){
            Properties properties = new Properties();
            properties.load(f);
            return properties;
        } catch (IOException e) {
            return null;
        }
    }
}
