package de.sean.splugin.discord;

/* SPlugin */
import de.sean.splugin.SPlugin;

/* Spigot */
import org.bukkit.configuration.file.FileConfiguration;

/* Discord */
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

/* Java */
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

public class DiscordUtil {
    private JDA jda;
    public static final Map<Guild, TextChannel> channels = new HashMap<>();
    public final boolean joinMessage;
    public final boolean leaveMessage;

    public DiscordUtil(FileConfiguration config) throws NullPointerException {
        final String token = config.getString("discord.token");
        this.joinMessage = config.getBoolean("discord.joinMessage");
        this.leaveMessage = config.getBoolean("discord.leaveMessage");
        // Only initialize discord stuff if a guild, channel and token are present.
        if (token != null) {
            final JDABuilder builder = new JDABuilder(token);
            try {
                builder.setActivity(Activity.playing("Minecraft"));
                this.jda = builder.build();
                this.jda.awaitReady();
                this.jda.addEventListener(new SHandler()); // Add event listeners for discord
                SPlugin.instance.getLogger().info("Discord has started successfully!");
            } catch (LoginException | InterruptedException e) {
                e.printStackTrace();
            }

            for (Map.Entry<String, Object> pair : config.getConfigurationSection("discord.channels").getValues(true).entrySet()) {
                final Guild guild = jda.getGuildById(pair.getKey());
                if (guild != null) {
                    final TextChannel channel = guild.getTextChannelById(pair.getValue().toString());
                    if (channel != null) channels.put(guild, channel);
                }
            }
        }
    }

    public void sendMessage(String message) {
        for (final Map.Entry<Guild, TextChannel> pair : channels.entrySet()) {
            pair.getValue().sendMessage(message).queue();
        }
    }
}