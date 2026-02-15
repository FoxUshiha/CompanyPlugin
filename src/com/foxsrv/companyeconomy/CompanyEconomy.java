package com.foxsrv.companyeconomy;

import com.foxsrv.companyeconomy.company.CompanyManager;
import com.foxsrv.companyeconomy.company.SalaryTask;
import com.foxsrv.companyeconomy.command.CompanyCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CompanyEconomy extends JavaPlugin {

    private static CompanyEconomy instance;
    private Economy economy;
    private CompanyManager companyManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        companyManager = new CompanyManager(this);
        companyManager.loadCompanies();

        getCommand("company").setExecutor(new CompanyCommand(this));
        getCommand("company").setTabCompleter(new CompanyCommand(this));

        new SalaryTask(this).runTaskTimer(this, 0L, 30 * 60 * 20L);
    }

    public static CompanyEconomy get() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public CompanyManager getCompanyManager() {
        return companyManager;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }
}
