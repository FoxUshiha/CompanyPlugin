package com.foxsrv.companyeconomy.company;

import com.foxsrv.companyeconomy.CompanyEconomy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SalaryTask extends BukkitRunnable {

    private final CompanyEconomy plugin;

    public SalaryTask(CompanyEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {

        plugin.getCompanyManager().getCompanies().forEach(company -> {

            company.getEmployees().forEach((playerName, groupId) -> {

                Player player = Bukkit.getPlayerExact(playerName);

                // Se offline, não paga
                if (player == null) return;

                double salary = company.getSalary(groupId);

                if (salary <= 0) return;

                if (company.getBalance() >= salary) {

                    plugin.getEconomy().depositPlayer(player, salary);
                    company.withdraw(salary);

                    player.sendMessage(ChatColor.GREEN +
                            "You received your salary: $" + salary);

                } else {

                    player.sendMessage(ChatColor.RED +
                            "You did not receive salary because the company you are employed in has not enough money.");
                }
            });
        });
    }
}
