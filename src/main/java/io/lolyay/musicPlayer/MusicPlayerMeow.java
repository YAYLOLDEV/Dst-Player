package io.lolyay.musicPlayer;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.lolyay.musicPlayer.commands.*;
import io.lolyay.musicPlayer.music.MusicManager;
import io.lolyay.musicPlayer.music.VoiceChatPlugin;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

public final class MusicPlayerMeow extends JavaPlugin {
    @Getter
    private static MusicPlayerMeow instance;
    public MusicManager musicManager;

    public MusicPlayerMeow() {
        super();
        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;
        musicManager = new MusicManager(
                 this.getConfig().getLong("server-id", 541412412L), // random yes
                 this.getConfig().getInt("resample-default-volume", 40)
        );
        int pluginId = 30021;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(
                new SingleLineChart("players", () -> {
                    try {
                        return musicManager.getClient().getPlayerStatusMap().size();
                    } catch (Exception ignored) { }
                    return -1;
                })
        );
        metrics.addCustomChart(
                new SingleLineChart("clients", () -> {
                    try {
                        return musicManager.getClient().getServerStatus().clients();
                    } catch (Exception ignored) {
                    }
                    return -1;
                })
        );

        // Plugin startup logic
        //TODO make radio the same multicommand as /music eg /music radio play
        this.registerCommand("radio", new RadioPlayCommand());
        this.registerCommand("radiostop", new RadioStopCommand());
        this.registerCommand("music", new MusicCommand());
        this.registerCommand("mp", new MpPlayerCommand());

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new VoiceChatPlugin());
        }
        this.saveDefaultConfig();
        this.musicManager.start();



    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
