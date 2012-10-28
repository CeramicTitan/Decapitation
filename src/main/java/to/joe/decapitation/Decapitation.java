package to.joe.decapitation;

import java.sql.SQLException;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Decapitation extends JavaPlugin implements Listener {

    public static final int HEAD = 397;
    public static final int HEADBLOCK = 144;
    double allDeaths;
    double killedByPlayer;
    boolean bounties = false;
    double tax;
    MySQL sql;

    public static Economy economy = null;

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        allDeaths = getConfig().getDouble("dropSkulls.allDeaths");
        killedByPlayer = getConfig().getDouble("dropSkulls.killedByPlayer");
        tax = getConfig().getDouble("bounty.tax");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("setname").setExecutor(new SetNameCommand());
        getCommand("spawnhead").setExecutor(new SpawnHeadCommand());
        getCommand("bounty").setExecutor(new BountyCommand(this));
        if (getConfig().getBoolean("bounties.enabled"))
            bounties = setupEconomy();
        if (bounties) {
            try {
                sql = new MySQL(getConfig().getString("mysql.url"), getConfig().getString("mysql.username"), getConfig().getString("mysql.password"));
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Error connecting to mysql database", e);
                bounties = false;
            }
        }
        if (bounties)
            getLogger().info("Bounties enabled");
        else
            getLogger().info("Bounties not enabled");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!event.isCancelled() && event.getBlock().getTypeId() == HEADBLOCK && event.getBlock().getData() == 3) {
            Head oldHead = new Head((CraftItemStack) event.getBlock());
            if (oldHead.isNamed()) {
                String name = oldHead.getName();
                CraftItemStack c = new CraftItemStack(HEAD, 1, (short) 0, (byte) 3);
                new Head(c).setName(name);
                event.setCancelled(true);
                Block b = event.getBlock();
                b.setTypeId(0);
                b.getWorld().dropItemNaturally(b.getLocation(), c);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        Player k = p.getKiller();
        if (p.hasPermission("decapitation.dropheads") && (allDeaths > Math.random() || (killedByPlayer > Math.random()) && k != null) && k.hasPermission("decapitation.collectheads")) {
            CraftItemStack c = new CraftItemStack(HEAD, 1, (short) 0, (byte) 3);
            new Head(c).setName(event.getEntity().getName());
            event.getDrops().add(c);
        }
    }

}