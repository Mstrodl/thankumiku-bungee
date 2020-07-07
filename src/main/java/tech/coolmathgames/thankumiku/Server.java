package tech.coolmathgames.thankumiku;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

enum SERVER_STATUS {
  ON,
  STARTING,
  OFF
};

public class Server {
    ServerInfo info;

  SERVER_STATUS status;
//  Exec details
  List<String> command;
  File cwd;
//  Shutdown after X
  Long shutdownTime;
  Process process;
  TimeoutThread timeout;
  ThankuMiku core;

//  Evil reflection
  Field addressField;
  SocketAddress realAddress; // Actual server address
  SocketAddress fakeAddress; // Our query address

  Map<ProxiedPlayer, ServerConnectEvent.Reason> queue;

  Server(ThankuMiku miku, ServerInfo info, List<String> command, File cwd, Long shutdownTime) {
    this.core = miku;
    this.info = info;
    this.command = command;
    this.cwd = cwd;
    this.shutdownTime = shutdownTime;
    this.status = SERVER_STATUS.OFF;
    this.queue = new HashMap();

    this.realAddress = this.info.getSocketAddress();
    this.fakeAddress = new InetSocketAddress("127.0.0.1", this.core.queryPort);

    this.core.getLogger().info(this.info.getClass().toString());
    try {
      this.addressField = this.info.getClass().getDeclaredField("socketAddress");
      this.addressField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }

    //this.syncQuery();
  }

  public void syncQuery() {
    try {
      if (this.status == SERVER_STATUS.ON) {
        this.addressField.set(this.info, this.realAddress);
      } else {
        this.addressField.set(this.info, this.fakeAddress);
      }
    } catch(IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public void start() {
    this.start(false);
  }

  public void start(Boolean override) {
    if(!override && this.status != SERVER_STATUS.OFF) {
      throw new Error("Server " + this.info.getName() + " isn't off but we were requested to start it!");
    }
    this.status = SERVER_STATUS.STARTING;
    if(this.process != null) {
      Server self = this;
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            self.process.waitFor();
          } catch(InterruptedException e) {
            e.printStackTrace();
          }
//          Start now that it's safe!
          self.start(true);
        }
      });
      thread.start();
      return;
    }

    this.core.getLogger().info("Spawning server " + this.info.getName());

    ProcessBuilder pb = new ProcessBuilder(this.command);
    pb.directory(this.cwd);
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
    try {
      this.process = pb.start();
    } catch(IOException e) {
      e.printStackTrace();
    }

    this.core.getLogger().info("Started process, waiting until startup!");

    InputStream stdout = this.process.getInputStream();

    Server self = this;

    Thread lineThread = new Thread(new Runnable() {
      @Override
      public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        String line;
        try {
          while ((line = reader.readLine()) != null) {
            if (line.contains("For help, type \"help\"")) {
              self.status = SERVER_STATUS.ON;
              self._isOn();
            }
            System.out.println(line);
          }
        } catch(IOException e) {
//          Just means process is dead, that's fine
        }
      }
    });
    Thread exitThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          self.process.waitFor();
        } catch(InterruptedException e) {
          e.printStackTrace();
        }
        self.process = null;
        self.status = SERVER_STATUS.OFF;
      }
    });
    exitThread.start();
    lineThread.start();
  }

  public void shutdown() {
    this.status = SERVER_STATUS.OFF;
    if(this.process != null) {
//    Sends SIGTERM
      this.process.destroy();
    }
  }

  public void enqueue(ProxiedPlayer player, ServerConnectEvent.Reason reason) {
    this.queue.put(player, reason);
    if(this.timeout != null) {
      this.timeout.cancel();
    }
  }
  public void dequeue(ProxiedPlayer player) {
    this.queue.remove(player);

    this.core.getLogger().info(
      "Dequeueing player " + player.getName() + " from server " + this.info.getName() + " Queue size is now " + this.queue.size()
    );
    this.core.getLogger().info("HI I STILl EXIST1");
    for(ProxiedPlayer queuePlayer : this.queue.keySet()) {
      this.core.getLogger().info("HI I STILl EXIST");
      this.core.getLogger().info("Still in queue: " + queuePlayer.getName());
    }
    this.core.getLogger().info("Thing!");
    if(this.queue.size() == 0) {
      Server self = this;
      this.timeout = new TimeoutThread("Timeout", this);
      this.timeout.start();
    }
  }

  public void _isOn() {
    for(ProxiedPlayer player : this.queue.keySet()) {
      player.connect(this.info, this.queue.get(player));
    }
  }
}
