package com.nhulston.essentials;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.nhulston.essentials.commands.back.BackCommand;
import com.nhulston.essentials.commands.freecam.FreecamCommand;
import com.nhulston.essentials.commands.god.GodCommand;
import com.nhulston.essentials.commands.heal.HealCommand;
import com.nhulston.essentials.commands.home.DelHomeCommand;
import com.nhulston.essentials.commands.home.HomeCommand;
import com.nhulston.essentials.commands.home.SetHomeCommand;
import com.nhulston.essentials.commands.kit.KitCommand;
import com.nhulston.essentials.commands.list.ListCommand;
import com.nhulston.essentials.commands.msg.MsgCommand;
import com.nhulston.essentials.commands.msg.ReplyCommand;
import com.nhulston.essentials.commands.rtp.RtpCommand;
import com.nhulston.essentials.commands.top.TopCommand;
import com.nhulston.essentials.commands.tphere.TphereCommand;
import com.nhulston.essentials.commands.spawn.SetSpawnCommand;
import com.nhulston.essentials.commands.spawn.SpawnCommand;
import com.nhulston.essentials.commands.tpa.TpaCommand;
import com.nhulston.essentials.commands.tpa.TpacceptCommand;
import com.nhulston.essentials.commands.warp.DelWarpCommand;
import com.nhulston.essentials.commands.warp.SetWarpCommand;
import com.nhulston.essentials.commands.warp.WarpCommand;
import com.nhulston.essentials.events.BuildProtectionEvent;
import com.nhulston.essentials.events.ChatEvent;
import com.nhulston.essentials.events.DeathLocationEvent;
import com.nhulston.essentials.events.MotdEvent;
import com.nhulston.essentials.events.PlayerQuitEvent;
import com.nhulston.essentials.events.SpawnProtectionEvent;
import com.nhulston.essentials.events.SpawnRegionTitleEvent;
import com.nhulston.essentials.events.SpawnTeleportEvent;
import com.nhulston.essentials.events.TeleportMovementEvent;
import com.nhulston.essentials.events.SleepPercentageEvent;
import com.nhulston.essentials.events.UpdateNotifyEvent;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.ChatManager;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.managers.SpawnProtectionManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.StorageManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.VersionChecker;

import javax.annotation.Nonnull;

public class Essentials extends JavaPlugin {
    public static final String VERSION = "1.4.1";
    
    private ConfigManager configManager;
    private StorageManager storageManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private ChatManager chatManager;
    private SpawnProtectionManager spawnProtectionManager;
    private TpaManager tpaManager;
    private TeleportManager teleportManager;
    private KitManager kitManager;
    private BackManager backManager;
    private VersionChecker versionChecker;

    public Essentials(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        Log.init(getLogger());
        Log.info("Essentials is starting...");

        configManager = new ConfigManager(getDataDirectory());
        storageManager = new StorageManager(getDataDirectory());

        homeManager = new HomeManager(storageManager, configManager);
        warpManager = new WarpManager(storageManager);
        spawnManager = new SpawnManager(storageManager);
        chatManager = new ChatManager(configManager);
        spawnProtectionManager = new SpawnProtectionManager(configManager, storageManager);
        tpaManager = new TpaManager();
        teleportManager = new TeleportManager(configManager);
        kitManager = new KitManager(getDataDirectory(), storageManager);
        backManager = new BackManager();
        versionChecker = new VersionChecker(VERSION);
    }

    @Override
    protected void start() {
        registerCommands();
        registerEvents();
        
        // Check for updates asynchronously
        versionChecker.checkForUpdatesAsync();
        
        Log.info("Essentials v" + VERSION + " started successfully!");
    }

    @Override
    protected void shutdown() {
        Log.info("Essentials is shutting down...");

        if (storageManager != null) {
            storageManager.shutdown();
        }

        if (tpaManager != null) {
            tpaManager.shutdown();
        }

        if (teleportManager != null) {
            teleportManager.shutdown();
        }

        Log.info("Essentials shut down.");
    }

    private void registerCommands() {
        // Home commands
        getCommandRegistry().registerCommand(new SetHomeCommand(homeManager));
        getCommandRegistry().registerCommand(new HomeCommand(homeManager, teleportManager));
        getCommandRegistry().registerCommand(new DelHomeCommand(homeManager));

        // Warp commands
        getCommandRegistry().registerCommand(new SetWarpCommand(warpManager));
        getCommandRegistry().registerCommand(new WarpCommand(warpManager, teleportManager));
        getCommandRegistry().registerCommand(new DelWarpCommand(warpManager));

        // Spawn commands
        getCommandRegistry().registerCommand(new SetSpawnCommand(spawnManager));
        getCommandRegistry().registerCommand(new SpawnCommand(spawnManager, teleportManager));

        // TPA commands
        getCommandRegistry().registerCommand(new TpaCommand(tpaManager));
        getCommandRegistry().registerCommand(new TpacceptCommand(tpaManager, teleportManager));

        // Kit command
        getCommandRegistry().registerCommand(new KitCommand(kitManager));

        // Back command
        getCommandRegistry().registerCommand(new BackCommand(backManager, teleportManager));

        // RTP command
        getCommandRegistry().registerCommand(new RtpCommand(configManager, teleportManager));

        // List command
        getCommandRegistry().registerCommand(new ListCommand());

        // Heal command
        getCommandRegistry().registerCommand(new HealCommand());

        // Freecam command
        getCommandRegistry().registerCommand(new FreecamCommand());

        // God command
        getCommandRegistry().registerCommand(new GodCommand());

        // Msg command (with aliases: m, message, whisper, pm)
        getCommandRegistry().registerCommand(new MsgCommand());

        // Reply command (with alias: reply)
        getCommandRegistry().registerCommand(new ReplyCommand());

        // Tphere command
        getCommandRegistry().registerCommand(new TphereCommand());

        // Top command
        getCommandRegistry().registerCommand(new TopCommand());
    }

    private void registerEvents() {
        new ChatEvent(chatManager).register(getEventRegistry());
        new BuildProtectionEvent(configManager).register(getEntityStoreRegistry());
        new SpawnProtectionEvent(spawnProtectionManager).register(getEntityStoreRegistry());
        new SpawnRegionTitleEvent(spawnProtectionManager, configManager).register(getEntityStoreRegistry());
        new TeleportMovementEvent(teleportManager).register(getEntityStoreRegistry());

        SpawnTeleportEvent spawnTeleportEvent = new SpawnTeleportEvent(spawnManager, configManager, storageManager);
        spawnTeleportEvent.registerEvents(getEventRegistry());
        spawnTeleportEvent.registerSystems(getEntityStoreRegistry());

        // Death location tracking for /back
        new DeathLocationEvent(backManager).register(getEntityStoreRegistry());

        // MOTD on join
        new MotdEvent(configManager).register(getEventRegistry());

        // Update notification for admins
        new UpdateNotifyEvent(versionChecker).register(getEventRegistry());

        // Sleep percentage system
        new SleepPercentageEvent(configManager).register(getEntityStoreRegistry());

        // Player disconnect cleanup
        new PlayerQuitEvent(storageManager, tpaManager, teleportManager, backManager).register(getEventRegistry());

        // Sync spawn provider with world config after all worlds are loaded
        // This updates the spawn marker on the map
        getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, event -> {
            spawnManager.syncWorldSpawnProvider();
        });
    }
}
