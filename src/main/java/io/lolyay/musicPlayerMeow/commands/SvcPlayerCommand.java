package io.lolyay.qaDBLSBUtils.commands;

import github.scarsz.discordsrv.dependencies.alexh.Fluent;
import io.lolyay.discordmsend.client.PlayerStatus;
import io.lolyay.discordmsend.client.ServerStatus;
import io.lolyay.qaDBLSBUtils.QaDBLSBUtils;
import io.lolyay.qaDBLSBUtils.music.MusicEventHandler;
import io.lolyay.qaDBLSBUtils.music.MusicManager;
import io.lolyay.qaDBLSBUtils.music.VoiceChatPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.lolyay.qaDBLSBUtils.QaDBLSBUtils.getInstance;

public class SvcPlayerCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String[] strings) {
        if(strings.length != 1){
            commandSourceStack.getSender().sendMessage("Usage: /svc reconnect|status");
            return;
        }
        if(strings[0].equals("reconnect")){
            commandSourceStack.getSender().sendMessage("Reconnecting...");
            getInstance().musicManager.getClient().disconnect("Reconnecting...");
            getInstance().musicManager.getClient().connect("localhost", 2677);
            getInstance().musicManager.getClient().waitUntilConnected();
            getInstance().musicManager.getClient().setDefaultVolume(15);
            commandSourceStack.getSender().sendMessage("Reconnected");
        } else if(strings[0].equals("status")){
            ServerStatus serverStatus = getInstance().musicManager.getClient().getServerStatus();
            TextComponent component = Component.text("DST Status: ").appendNewline()
                    .append(Component.text("Server ID: " + getInstance().musicManager.getServerId()).appendNewline())
                    .append(Component.text("Players: " + serverStatus.players()).appendNewline())
                    .append(Component.text("Clients: " + serverStatus.clients()).appendNewline())
                    .append(Component.text("DST CPU Usage: " + serverStatus.cpuUsageP() + "%").appendNewline())
                    .append(Component.text("DST Free Memory: " + serverStatus.freeMem()).appendNewline())
                    .append(Component.text("DST Max Memory: " + serverStatus.maxMem()).appendNewline());
            for(Map.Entry<Long, PlayerStatus> status : getInstance().musicManager.getClient().getPlayerStatusMap().entrySet()){
                component = component.append(Component.text("----------------").appendNewline());
                component = component.append(Component.text("Player Status for: " + status.getKey()).appendNewline());
                component = component.append(Component.text("Position: " + status.getValue().position()).appendNewline());
                component = component.append(Component.text("TrackID: " + status.getValue().trackId()).appendNewline());
                component = component.append(Component.text("Volume: " + status.getValue().volume()).appendNewline());
            }
            commandSourceStack.getSender().sendMessage(component);
        }
    }

    @Override
    public @Nullable String permission() {
        return "qaDBLSBUtils.music.reconnect";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        List<String> r = List.of("reconnect", "status");
        if(args.length == 1){
            r = r.stream().filter(s -> s.startsWith(args[0])).toList();
        }
        return r;
    }
}
