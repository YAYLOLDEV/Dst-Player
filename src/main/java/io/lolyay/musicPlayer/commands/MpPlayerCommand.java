package io.lolyay.musicPlayer.commands;

import io.lolyay.discordmsend.client.PlayerStatus;
import io.lolyay.discordmsend.client.ServerStatus;
import io.lolyay.musicPlayer.MusicPlayerMeow;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;


public class MpPlayerCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String[] strings) {
        if(strings.length != 1){
            commandSourceStack.getSender().sendMessage("Usage: /mp reconnect|status|debug");
            return;
        }
        if(strings[0].equals("reconnect")){
            commandSourceStack.getSender().sendMessage("Reconnecting...");
            MusicPlayerMeow.getInstance().musicManager.getClient().disconnect("Reconnecting...");
            String host = MusicPlayerMeow.getInstance().getConfig().getString("music-host");
            int port = MusicPlayerMeow.getInstance().getConfig().getInt("music-port");
            MusicPlayerMeow.getInstance().musicManager.getClient().connect(host, port);
            MusicPlayerMeow.getInstance().musicManager.getClient().waitUntilConnected();
            try {
                Thread.sleep(500); // server has race condition here, better wait
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            MusicPlayerMeow.getInstance().musicManager.getClient().setDefaultVolume(100);
            commandSourceStack.getSender().sendMessage("Reconnected");
        } else if(strings[0].equals("status")){
            ServerStatus serverStatus = MusicPlayerMeow.getInstance().musicManager.getClient().getServerStatus();
            TextComponent component = Component.text("DST Status: ").appendNewline()
                    .append(Component.text("Server ID: " + MusicPlayerMeow.getInstance().musicManager.getServerId()).appendNewline())
                    .append(Component.text("Players: " + serverStatus.players()).appendNewline())
                    .append(Component.text("Clients: " + serverStatus.clients()).appendNewline())
                    .append(Component.text("DST CPU Usage: " + serverStatus.cpuUsageP() + "%").appendNewline())
                    .append(Component.text("DST Free Memory: " + serverStatus.freeMem()).appendNewline())
                    .append(Component.text("DST Max Memory: " + serverStatus.maxMem()).appendNewline());
            for(Map.Entry<Long, PlayerStatus> status : MusicPlayerMeow.getInstance().musicManager.getClient().getPlayerStatusMap().entrySet()){
                component = component.append(Component.text("----------------").appendNewline());
                component = component.append(Component.text("Player Status for: " + status.getKey()).appendNewline());
                component = component.append(Component.text("Position: " + status.getValue().position()).appendNewline());
                component = component.append(Component.text("TrackID: " + status.getValue().trackId()).appendNewline());
                component = component.append(Component.text("Volume: " + status.getValue().volume()).appendNewline());
            }
            commandSourceStack.getSender().sendMessage(component);
        } else if(strings[0].equals("debug")){
            boolean debug = !MusicPlayerMeow.getInstance().musicManager.isDebug;
            MusicPlayerMeow.getInstance().musicManager.isDebug = debug;
            commandSourceStack.getSender().sendMessage("Debug mode: " + (debug ? "ENABLED" : "DISABLED"));
        }
    }

    @Override
    public @Nullable String permission() {
        return "qaDBLSBUtils.music.reconnect";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        List<String> r = List.of("reconnect", "status", "debug");
        if(args.length == 1){
            r = r.stream().filter(s -> s.startsWith(args[0])).toList();
        }
        return r;
    }
}
