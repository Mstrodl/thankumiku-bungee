package tech.coolmathgames.thankumiku;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Map;

public class Events implements Listener {
    private ThankuMiku core;

    public Events(ThankuMiku core) {
        this.core = core;
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        this.core.getLogger().info("onServerSwitch fired");
        ServerInfo from = event.getFrom();

        if(from != null && this.core.servers.containsKey(from.getName())) {
            ProxiedPlayer player = event.getPlayer();
            Server server = this.core.servers.get(from.getName());
            server.dequeue(player);
        }
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent event) {
        this.core.getLogger().info("onServerDisconnect fired");
        ServerInfo from = event.getTarget();
        if(this.core.servers.containsKey(from.getName())) {
            ProxiedPlayer player = event.getPlayer();
            Server server = this.core.servers.get(from.getName());
            server.dequeue(player);
        }
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        this.core.getLogger().info("Sending user to: " + event.getTarget().getName());

        if(event.getTarget().getName().equalsIgnoreCase(this.core.waitingServer.getName())) {
            for(Server server : this.core.servers.values()) {
                if(server.queue.containsKey(event.getPlayer())) {
                    return;
                }
            }
            Server server = this.core.servers.values().iterator().next();
            this.core.getLogger().info("Redirecting player " + event.getPlayer().getName() + " to " + server.info.getName());
            event.setTarget(server.info);
        }
        ProxiedPlayer player = event.getPlayer();

        String name = event.getTarget().getName();
        this.core.getLogger().info("Sending to " + name);
        if(this.core.servers.containsKey(name)) {
            Server server = this.core.servers.get(name);

            if(server.status != SERVER_STATUS.ON) {
                this.core.getLogger().info("Sending player " + player.getName() + " to waiting server");
                if(server.status == SERVER_STATUS.OFF) {
                    server.start();
                }
                server.enqueue(player, event.getReason());

                if(player.getServer() != null && player.getServer().getInfo().equals(this.core.waitingServer)) {
                    // Already in the waiting server, cancel event
                    event.setCancelled(true);
                } else {
                    event.setTarget(this.core.waitingServer);
                }
            }
        }
    }
}
