package io.lolyay.musicPlayerMeow;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.lolyay.musicPlayerMeow.commands.*;
import io.lolyay.musicPlayerMeow.music.MusicManager;
import io.lolyay.musicPlayerMeow.music.VoiceChatPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
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
                243595873 // random yes
        );
        // Plugin startup logic
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
