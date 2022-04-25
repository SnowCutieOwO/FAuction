package fr.florianpal.fauction;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.zaxxer.hikari.HikariDataSource;
import fr.florianpal.fauction.commands.AuctionCommand;
import fr.florianpal.fauction.schedules.ExpireSchedule;
import fr.florianpal.fauction.managers.ConfigurationManager;
import fr.florianpal.fauction.managers.DatabaseManager;
import fr.florianpal.fauction.managers.VaultIntegrationManager;
import fr.florianpal.fauction.managers.commandManagers.AuctionCommandManager;
import fr.florianpal.fauction.managers.commandManagers.CommandManager;
import fr.florianpal.fauction.managers.commandManagers.ExpireCommandManager;
import fr.florianpal.fauction.managers.commandManagers.LimitationManager;
import fr.florianpal.fauction.queries.AuctionQueries;
import fr.florianpal.fauction.queries.ExpireQueries;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class FAuction extends JavaPlugin {

    private HikariDataSource hikari;
    private static TaskChainFactory taskChainFactory;

    private ConfigurationManager configurationManager;
    private AuctionQueries auctionQueries;
    private ExpireQueries expireQueries;

    private ExpireSchedule expireListener;

    private CommandManager commandManager;
    private VaultIntegrationManager vaultIntegrationManager;
    private DatabaseManager databaseManager;
    private LimitationManager limitationManager;

    private AuctionCommandManager auctionCommandManager;
    private ExpireCommandManager expireCommandManager;

    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }

    public static TaskChainFactory getTaskChainFactory() {
        return taskChainFactory;
    }

    @Override
    public void onEnable() {
        PaperLib.suggestPaper(this);

        taskChainFactory = BukkitTaskChainFactory.create(this);

        File languageFile = new File(getDataFolder(), "lang_fr.yml");
        createDefaultConfiguration(languageFile, "lang_fr.yml");

        configurationManager = new ConfigurationManager(this);
        commandManager = new CommandManager(this);
        commandManager.registerDependency(ConfigurationManager.class, configurationManager);

        limitationManager = new LimitationManager(this);

        vaultIntegrationManager = new VaultIntegrationManager(this);

        databaseManager = new DatabaseManager(this);
        auctionQueries = new AuctionQueries(this);
        expireQueries = new ExpireQueries(this);

        databaseManager.addRepository(expireQueries);
        databaseManager.addRepository(auctionQueries);
        databaseManager.initializeTables();

        auctionCommandManager = new AuctionCommandManager(this);
        expireCommandManager = new ExpireCommandManager(this);

        commandManager.registerCommand(new AuctionCommand(this));

        expireListener = new ExpireSchedule(this);

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, expireListener, configurationManager.getGlobalConfig().getCheckEvery(), configurationManager.getGlobalConfig().getCheckEvery());
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public AuctionQueries getAuctionQueries() {
        return auctionQueries;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public VaultIntegrationManager getVaultIntegrationManager() {
        return vaultIntegrationManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void createDefaultConfiguration(File actual, String defaultName) {
        // Make parent directories
        File parent = actual.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        if (actual.exists()) {
            return;
        }

        InputStream input = null;
        try {
            JarFile file = new JarFile(this.getFile());
            ZipEntry copy = file.getEntry(defaultName);
            if (copy == null) throw new FileNotFoundException();
            input = file.getInputStream(copy);
        } catch (IOException e) {
            getLogger().severe("Unable to read default configuration: " + defaultName);
        }

        if (input != null) {
            FileOutputStream output = null;

            try {
                output = new FileOutputStream(actual);
                byte[] buf = new byte[8192];
                int length;
                while ((length = input.read(buf)) > 0) {
                    output.write(buf, 0, length);
                }

                getLogger().info("Default configuration file written: " + actual.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    input.close();
                } catch (IOException ignored) {
                }

                try {
                    if (output != null) {
                        output.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    public AuctionCommandManager getAuctionCommandManager() {
        return auctionCommandManager;
    }

    public LimitationManager getLimitationManager() {
        return limitationManager;
    }

    public ExpireQueries getExpireQueries() {
        return expireQueries;
    }

    public ExpireCommandManager getExpireCommandManager() {
        return expireCommandManager;
    }
}
