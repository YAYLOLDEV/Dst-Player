package io.lolyay.musicPlayer.commands;

import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.musicPlayer.MusicPlayerMeow;
import io.lolyay.musicPlayer.PlayerID;
import io.lolyay.musicPlayer.music.MusicEventHandler;
import io.lolyay.musicPlayer.music.PersonalMusicSender;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class MusicCommand implements BasicCommand {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[a-zA-Z]{1,5}:.*");

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String[] strings) {
        if (!(commandSourceStack.getExecutor() instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                    Component.text("Only players can use music commands!", NamedTextColor.RED)
            );
            return;
        }

        if (strings.length == 0) {
            sendHelp(player);
            return;
        }

        String subCommand = strings[0].toLowerCase();

        switch (subCommand) {
            case "play":
                handlePlay(player, strings);
                break;
            case "info":
                handleInfo(player);
                break;
            case "stop":
                handleStop(player);
                break;
            case "pause":
                handlePause(player);
                break;
            case "resume":
                handleResume(player);
                break;
            default:
                sendHelp(player);
                break;
        }
    }

    private void handlePlay(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /music play <query>", NamedTextColor.RED));
            return;
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            queryBuilder.append(args[i]).append(" ");
        }
        String query = queryBuilder.toString().trim();

        //Horrible check, youtube.be/videoid is supported by DAPI but can't pass this check
        if (!(PREFIX_PATTERN.matcher(query).matches() || query.startsWith("https://"))) {
            query = "ytm:" + query;
        }

        MusicPlayerMeow.getInstance().musicManager.stopPersonalMusic(player.getUniqueId());

        long guildId = MusicPlayerMeow.getInstance().musicManager.getServerId() + PlayerID.toInt(player.getUniqueId());
        PersonalMusicSender sender = MusicEventHandler.getPersonalPlayer(guildId);
        if (sender != null) {
            sender.clean();
        }

        MusicPlayerMeow.getInstance().musicManager.playPersonalSong(player.getUniqueId(), query, player.getName())
                .thenAccept(trackInfo -> {
                    player.sendMessage(
                            Component.text("🎵 Now playing: ", NamedTextColor.GREEN)
                                    .append(Component.text(trackInfo.trackName(), NamedTextColor.AQUA))
                                    .append(Component.text(" by ", NamedTextColor.GREEN))
                                    .append(Component.text(trackInfo.author(), NamedTextColor.AQUA))
                    );
                }).exceptionally(ex -> {
                    player.sendMessage(
                            Component.text("Failed to play music: " + ex.getMessage(), NamedTextColor.RED)
                    );
                    return null;
                });
    }

    private void handleInfo(Player player) {
        long guildId = MusicPlayerMeow.getInstance().musicManager.getServerId() + PlayerID.toInt(player.getUniqueId());
        PersonalMusicSender sender = MusicEventHandler.getPersonalPlayer(guildId);
        
        if (sender != null && sender.getCurrentTrack() != null) {
            TrackMetadata info = sender.getCurrentTrack();
            player.sendMessage(Component.text("--------------------------------", NamedTextColor.DARK_PURPLE));
            player.sendMessage(Component.text("🎵 Current Track Info 🎵", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Title: ", NamedTextColor.GRAY).append(Component.text(info.trackName(), NamedTextColor.AQUA)));
            player.sendMessage(Component.text("Author: ", NamedTextColor.GRAY).append(Component.text(info.author(), NamedTextColor.AQUA)));
            player.sendMessage(Component.text("Duration: ", NamedTextColor.GRAY).append(Component.text(formatDuration(info.durationMs()), NamedTextColor.GOLD)));
            player.sendMessage(Component.text("Id: ", NamedTextColor.GRAY).append(Component.text(info.id(), NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("--------------------------------", NamedTextColor.DARK_PURPLE));
        } else {
            player.sendMessage(Component.text("No music is currently playing.", NamedTextColor.YELLOW));
        }
    }

    private void handleStop(Player player) {
        MusicPlayerMeow.getInstance().musicManager.stopPersonalMusic(player.getUniqueId());
        MusicEventHandler.stopPersonalAudioPlayer(player.getUniqueId());
        player.sendMessage(Component.text("Stopped music playback.", NamedTextColor.GRAY));
    }

    private void handlePause(Player player) {
        // Local pause only
        long guildId = MusicPlayerMeow.getInstance().musicManager.getServerId() + PlayerID.toInt(player.getUniqueId());
        PersonalMusicSender sender = MusicEventHandler.getPersonalPlayer(guildId);
        if (sender != null) {
            sender.pause();
            player.sendMessage(Component.text("Paused local playback.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Nothing to pause.", NamedTextColor.RED));
        }
    }

    private void handleResume(Player player) {
        // Local resume only
        long guildId = MusicPlayerMeow.getInstance().musicManager.getServerId() + PlayerID.toInt(player.getUniqueId());
        PersonalMusicSender sender = MusicEventHandler.getPersonalPlayer(guildId);
        if (sender != null) {
            sender.resume();
            player.sendMessage(Component.text("Resumed local playback.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Nothing to resume.", NamedTextColor.RED));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("Music Commands:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/music play <query> - Play a song", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/music info - Show current song info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/music stop - Stop playback", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/music pause - Pause local playback", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/music resume - Resume local playback", NamedTextColor.YELLOW));
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public @Nullable String permission() {
        return "music.use";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length == 1) {
            return List.of("play", "info", "stop", "pause", "resume");
        }
        return List.of();
    }
}
