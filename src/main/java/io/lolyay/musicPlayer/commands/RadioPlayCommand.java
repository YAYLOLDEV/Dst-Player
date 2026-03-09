package io.lolyay.musicPlayer.commands;

import io.lolyay.musicPlayer.MusicPlayerMeow;
import io.lolyay.musicPlayer.music.VoiceChatPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;


public class RadioPlayCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String[] strings) {
        if (strings.length == 0) {
            commandSourceStack.getSender().sendMessage(
                Component.text("Usage: /radio <search query>", NamedTextColor.RED)
            );
            return;
        }
        
        String query = String.join(" ", strings);
        
        VoiceChatPlugin.addAllPlayersToGlobal();

        MusicPlayerMeow.getInstance().musicManager.playGlobalRadio(query).thenAccept(trackInfo -> {
            commandSourceStack.getSender().sendMessage(
                Component.text("🎵 Now playing on GLOBAL RADIO: ", NamedTextColor.GOLD)
                    .append(Component.text(trackInfo.trackName(), NamedTextColor.YELLOW))
                    .append(Component.text(" by ", NamedTextColor.GOLD))
                    .append(Component.text(trackInfo.trackAuthor(), NamedTextColor.YELLOW))
            );
        }).exceptionally(ex -> {
            commandSourceStack.getSender().sendMessage(
                Component.text("Failed to play radio: " + ex.getMessage(), NamedTextColor.RED)
            );
            return null;
        });
    }

    @Override
    public @Nullable String permission() {
        return "qaDBLSBUtils.radio.play";
    }
}
