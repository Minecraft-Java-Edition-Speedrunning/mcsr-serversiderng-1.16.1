# ServerSideRNG
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/voidxwalker)

ServerSideRNG is an open-source, lightweight Minecraft Fabric mod that helps to ensure the legitimacy of Minecraft Speedruns, achieved by calculating important random values on a verification server.

This mod has been developed with Minecraft Java Edition Speedrunning Moderation team and is both **legal** and encouraged for speedrun.com submissions.
ServerSideRNG requires you to have a Mojang account. Even though having a constant internet connection is recommended, you can still gain value with an unstable one.

By using ServerSideRNG you agree to your UUID (the public ID associated with your Minecraft username) to get uploaded to a private verification server. The data on the server is only visible to Speedrun.com Verifiers and Leaderboard mods.
## Usage
ServerSideRNG will automatically verify your runs without any player input required. After completing a run, make sure that the run has been uploaded. The run uploads automatically when:
- running the /seed command (gives a chat response)
- terminating the game
- being outside of a world for 10 seconds
- completing a SpeedrunIGT run (gives a chat response)
You can also manually upload your run via the "/serversiderng_uploadRun" command.
If you think your run might not have been uploaded, immediately open a help thread in the Minecraft Java Edition Speedrunning Discord.

## Speedrun.com Submissions
You can find a ZIP-File with your world name in ".minecraft/verification-zips". Include this file in your Speedrun.com submission like you include your world file (e. g. via google drive)

## Config
You can turn off the first three methods of uploading your run listed above via the ".minecraft/config/serversiderng/serversiderng.properties" file.
Turning any of these methods off is not recommended.

##Javadoc https://voidxwalker.github.io

## Authors

- [@Void_X_Walker](https://www.github.com/voidxwalker) (https://ko-fi.com/voidxwalker)
