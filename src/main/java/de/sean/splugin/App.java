package de.sean.splugin;

/* SPlugin */
import de.sean.splugin.discord.DiscordUtil;
import de.sean.splugin.spigot.commands.*;
import de.sean.splugin.spigot.events.*;
import de.sean.splugin.spigot.tasks.AfkChecker;
import de.sean.splugin.spigot.tasks.SleepChecker;
import de.sean.splugin.util.PlayerType;
import de.sean.splugin.util.SUtil;

/* Java */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import org.jetbrains.annotations.NotNull;

/* Spigot */
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin {
    private static DiscordUtil discord;

    private static App instance;

    @Override
    public void onEnable() {
        // When starting also update IP as it might have changed while the server was offline.
        updateIP();

        /* Config */
        (instance = this).saveDefaultConfig();
        FileConfiguration config = instance.getConfig();

        PlayerType.loadFromConfig(config);

        // When we're reloading we want to set default values to all players, as old data gets lost on a reload.
        for (Player player : this.getServer().getOnlinePlayers()) {
            SUtil.setLastActivityForPlayer(player.getUniqueId(), System.currentTimeMillis());
            SUtil.setPlayerAFK(player.getUniqueId(), false);
        }

        if (config.getBoolean("features.skipNight"))
            Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new SleepChecker(), 0L, 20L);
        if (config.getBoolean("features.afk"))
            Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new AfkChecker(), 0L, 20L);

        PluginManager pm = Bukkit.getServer().getPluginManager();
        registerEvents(pm);
        registerCommands();

        /* Discord */
        discord = new DiscordUtil(config);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    public void registerEvents(@NotNull PluginManager pm) {
        pm.registerEvents(new BlockBurnEvent(), this);      // Handles a block about to burn down
        pm.registerEvents(new BlockEvent(), this);          // Handles the placement/destruction of blocks by players
        pm.registerEvents(new DeathEvent(), this);          // Handles every player death event
        pm.registerEvents(new DismountEvent(), this);       // Handles every entity dismount
        pm.registerEvents(new ExplodeEvent(), this);        // Handles every explosion in the world
        pm.registerEvents(new InteractEvent(), this);       // Handles every block interaction by a player
        pm.registerEvents(new JoinEvent(), this);           // Handles every user join event
        pm.registerEvents(new LeaveEvent(), this);          // Handles every user leave event
        pm.registerEvents(new MessageEvent(), this);        // Handles every chat message event
        pm.registerEvents(new MoveEvent(), this);           // Handles every move of a player
    }

    public void updateIP() {
        new Thread(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "node .");
            String dir = "./freenom-update";
            if (!Files.exists(FileSystems.getDefault().getPath(dir), LinkOption.NOFOLLOW_LINKS)) return;
            processBuilder.directory(new File(dir));
            try {
                Process process = processBuilder.start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                }
                process.waitFor();
                bufferedReader.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void registerCommands() {
        getCommand("lock").setExecutor(new LockExecutor());
    }

    public File getConfigFile() {
        return new File(getDataFolder() + File.separator + "config.yml");
    }

    public DiscordUtil getDiscordUtil() { return discord; }

    public static App getInstance() { return instance; }
}
