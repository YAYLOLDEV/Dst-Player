package io.lolyay.qaDBLSBUtils.commands;

import io.lolyay.qaDBLSBUtils.music.MusicEventHandler;
import io.lolyay.qaDBLSBUtils.music.MusicManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import static io.lolyay.qaDBLSBUtils.QaDBLSBUtils.getInstance;

public class MStopCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String[] strings) {
        if (!(commandSourceStack.getExecutor() instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                Component.text("Only players can stop personal music!", NamedTextColor.RED)
            );
            return;
        }
        

        MusicEventHandler.stopPersonalAudioPlayer(player.getUniqueId());

        player.sendMessage(
            Component.text("Stopped any playing music.", NamedTextColor.GRAY)
        );
    }

    @Override
    public @Nullable String permission() {
        return "qaDBLSBUtils.music.stop";
    }
}
