package com.nhulston.essentials;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.nhulston.essentials.commands.home.DelHomeCommand;
import com.nhulston.essentials.commands.home.HomeCommand;
import com.nhulston.essentials.commands.home.SetHomeCommand;
import com.nhulston.essentials.commands.kit.KitCommand;
import com.nhulston.essentials.commands.kit.KitCommand;
import com.nhulston.essentials.commands.spawn.SetSpawnCommand;
import com.nhulston.essentials.commands.spawn.SpawnCommand;
import com.nhulston.essentials.commands.tpa.TpaCommand;
import com.nhulston.essentials.commands.tpa.TpacceptCommand;
import com.nhulston.essentials.commands.warp.DelWarpCommand;
import com.nhulston.essentials.commands.warp.SetWarpCommand;
import com.nhulston.essentials.commands.warp.WarpCommand;
import com.nhulston.essentials.events.BuildProtectionEvent;
import com.nhulston.essentials.events.ChatEvent;
import com.nhulston.essentials.events.SpawnProtectionEvent;
import com.nhulston.essentials.events.SpawnRegionTitleEvent;
import com.nhulston.essentials.events.SpawnTeleportEvent;
import com.nhulston.essentials.managers.ChatManager;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.managers.SpawnProtectionManager;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.StorageManager;
import com.nhulston.essentials.util.Log;

import javax.annotation.Nonnull;

public class Essentials extends JavaPlugin {
    private ConfigManager configManager;
    private StorageManager storageManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private SpawnManager spawnManager;
    private ChatManager chatManager;
    private SpawnProtectionManager spawnProtectionManager;
    private TpaManager tpaManager;

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
    }

    @Override
    protected void start() {
        registerCommands();
        registerEvents();
        Log.info("Essentials started successfully!");
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

        Log.info("Essentials shut down.");
    }

    private void registerCommands() {
        // Home commands
        getCommandRegistry().registerCommand(new SetHomeCommand(homeManager));
        getCommandRegistry().registerCommand(new HomeCommand(homeManager));
        getCommandRegistry().registerCommand(new DelHomeCommand(homeManager));

        // Warp commands
        getCommandRegistry().registerCommand(new SetWarpCommand(warpManager));
        getCommandRegistry().registerCommand(new WarpCommand(warpManager));
        getCommandRegistry().registerCommand(new DelWarpCommand(warpManager));

        // Spawn commands
        getCommandRegistry().registerCommand(new SetSpawnCommand(spawnManager));
        getCommandRegistry().registerCommand(new SpawnCommand(spawnManager));

        // TPA commands
        getCommandRegistry().registerCommand(new TpaCommand(tpaManager));
        getCommandRegistry().registerCommand(new TpacceptCommand(tpaManager));

        // Kit command
        getCommandRegistry().registerCommand(new KitCommand());
    }

    private void registerEvents() {
        new ChatEvent(chatManager).register(getEventRegistry());
        new BuildProtectionEvent(configManager).register(getEntityStoreRegistry());
        new SpawnProtectionEvent(spawnProtectionManager).register(getEntityStoreRegistry());
        new SpawnRegionTitleEvent(spawnProtectionManager, configManager).register(getEntityStoreRegistry());

        SpawnTeleportEvent spawnTeleportEvent = new SpawnTeleportEvent(spawnManager, configManager, storageManager);
        spawnTeleportEvent.registerEvents(getEventRegistry());
        spawnTeleportEvent.registerSystems(getEntityStoreRegistry());
    }
}
