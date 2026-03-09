package io.lolyay.musicPlayerMeow.commands;

import io.lolyay.musicPlayerMeow.MusicPlayerMeow;
import io.lolyay.musicPlayerMeow.music.MusicEventHandler;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;


public class RadioStopCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String[] strings) {
        // Stop client playback
        MusicPlayerMeow.getInstance().musicManager.stopGlobalRadio();
        
        // Stop AudioPlayer
        MusicEventHandler.stopGlobalAudioPlayer();
        
        commandSourceStack.getSender().sendMessage(
            Component.text("🔇 Global radio stopped", NamedTextColor.GRAY)
        );
    }

    @Override
    public @Nullable String permission() {
        return "qaDBLSBUtils.radio.stop";
    }
}
