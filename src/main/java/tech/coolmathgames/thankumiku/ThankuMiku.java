package tech.coolmathgames.thankumiku;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.*;
import tech.coolmathgames.thankumiku.server.MiniServer;
//import net.md_5.bungee.BungeeServerInfo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThankuMiku extends Plugin {
    public Configuration configuration;
    public Map<String, Server> servers;
    public ServerInfo waitingServer;

    public void loadConfig() {
        if(!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "config.yml");
        if(!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch(IOException e) {
            e.printStackTrace();
        }

        this.servers = new HashMap();

        Map<String, ServerInfo> allServers = getProxy().getServers();

        this.waitingServer = allServers.get(this.configuration.getString("waitingServer"));
        InetSocketAddress address = waitingServer.getAddress();
        Integer port = address.getPort();
        MiniServer miniServer = new MiniServer("MiniServer", port);
        miniServer.start();

//        Load all servers from config
        Collection<String> serverNames = this.configuration.getSection("servers").getKeys();
        for(String name : serverNames) {
            ServerInfo trueInfo = allServers.get(name);
            if(trueInfo == null) {
                this.getLogger().warning("Server " + name + " doesn't exist!");
                continue;
            }

            Configuration serverConfig = this.configuration.getSection("servers").getSection(name);

            List<String> command = serverConfig.getStringList("command");
            File cwd = new File(serverConfig.getString("cwd"));
            Long shutdownTime = serverConfig.getLong("shutdownTime");

            this.getLogger().info("Adding: " + command + " at " + cwd.getAbsolutePath() + " after " + shutdownTime.toString());

            Server server = new Server(
                    this,
                    trueInfo,
                    command,
                    cwd,
                    shutdownTime
            );
            this.getLogger().info("Adding: " + name);
            this.servers.put(name, server);

            ServerPing ping = new ServerPing();
            ping.setDescriptionComponent(new TextComponent(TextComponent.fromLegacyText("ThankuMiku Server")));
            ping.setFavicon(Favicon.create(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)));
            ServerPing.PlayerInfo[] preview = new ServerPing.PlayerInfo[] {};
            ping.setPlayers(new ServerPing.Players(1, 0, preview));
            ping.setVersion(new ServerPing.Protocol("ThankuMiku 1.16.1", 736));

            try {
                Field cachedPing = trueInfo.getClass().getDeclaredField("cachedPing");
                Field lastPing = trueInfo.getClass().getDeclaredField("lastPing");
                cachedPing.setAccessible(true);
                lastPing.setAccessible(true);
                lastPing.set(trueInfo, Long.MAX_VALUE);
                cachedPing.set(trueInfo, ping);
            } catch(NoSuchFieldException | IllegalAccessException e) {
                this.getLogger().info("Field doesn't exist, ThankuMiku servers may not work properly!");
            }
        }
    }

    @Override
    public void onEnable() {
        this.loadConfig();

        this.getProxy().getPluginManager().registerListener(this, new Events(this));

        this.getLogger().info("Ready to thank Miku!");
    }
}
