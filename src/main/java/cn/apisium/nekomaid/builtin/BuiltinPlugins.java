package cn.apisium.nekomaid.builtin;

import cn.apisium.nekomaid.NekoMaid;
import org.bukkit.plugin.PluginManager;

public final class BuiltinPlugins {
    private final Terminal terminal;
    private final FilesManager files;
    private final LocalWebFrontend localWebFrontend;
    private Scheduler scheduler;
    private Profiler profiler;
    public BuiltinPlugins(NekoMaid main) {
        new Dashboard(main);
        terminal = new Terminal(main);
        AiAssistant ai = new AiAssistant(main, terminal);
        localWebFrontend = new LocalWebFrontend(main, ai);
        PlayerList.init(main);
        ServerConfig.init(main);
        files = new FilesManager(main);
        main.getServer().getScheduler().runTask(main, () -> {
            new Plugins(main);
            scheduler = new Scheduler(main);
            PluginManager pm = main.getServer().getPluginManager();
            if (pm.getPlugin("Vault") != null) new Vault(main);
            new PlayerInventory(main);
            new Worlds(main);
            new Editors(main);
            profiler = new Profiler(main);
        });
    }
    public void disable() {
        localWebFrontend.disable();
        terminal.stop();
        files.disable();
        if (scheduler != null) scheduler.stop();
        if (profiler != null) profiler.stop();
    }
}
