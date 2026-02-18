package com.foxsrv.companyeconomy.command;

import com.foxsrv.companyeconomy.CompanyEconomy;
import com.foxsrv.companyeconomy.company.Company;
import com.foxsrv.companyeconomy.company.CompanyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CompanyCommand implements CommandExecutor, TabCompleter {

    private final CompanyEconomy plugin;
    private final CompanyManager manager;

    public CompanyCommand(CompanyEconomy plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCompanyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (args.length == 0) {
            return handleInfo(sender, null);
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "hire": return handleHire(sender, args);
            case "fire": return handleFire(sender, args);
            case "leave": return handleLeave(sender, args);
            case "deposit": return handleDeposit(sender, args);
            case "withdraw": return handleWithdraw(sender, args);
            case "reload": return handleReload(sender);
            case "info": return handleInfo(sender, args.length >= 2 ? args[1] : null);
            default:
                Company company = manager.getCompany(args[0]);
                if (company != null) return handleInfo(sender, args[0]);
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
        }
    }

    /* ========================== INFO ========================== */

    private boolean handleInfo(CommandSender sender, String companyName) {

        Company company = (companyName != null)
                ? manager.getCompany(companyName)
                : manager.getCompanies().stream()
                .sorted(Comparator.comparing(Company::getName))
                .findFirst().orElse(null);

        if (company == null) {
            sender.sendMessage(ChatColor.RED + "Company not found.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + company.getDisplayName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Balance: "
                + ChatColor.GREEN + "$" + company.getBalance());

        sender.sendMessage(ChatColor.YELLOW + "Members:");

        for (Map.Entry<String, Integer> entry : company.getEmployees().entrySet()) {

            String playerName = entry.getKey();
            int group = entry.getValue();

            String role = company.getGroupTags().stream()
                    .filter(tag -> company.getGroupIdByName(tag) == group)
                    .findFirst().orElse("Unknown");

            sender.sendMessage(ChatColor.GRAY + "- "
                    + playerName + ChatColor.DARK_GRAY + " (" + role + ")");
        }

        return true;
    }

    /* ========================== HIRE ========================== */

    private boolean handleHire(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player executor = (Player) sender;

        if (args.length < 4) {
            executor.sendMessage(ChatColor.RED +
                    "Usage: /company hire <player> <company> <role>");
            return true;
        }

        String targetName = args[1];
        String companyName = args[2];
        String roleName = args[3];

        Company company = manager.getCompany(companyName);

        if (company == null ||
                !company.hasPermission(executor.getName(), "can-hire") ||
                !company.isEmployee(executor.getName())) {
            executor.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (company.isEmployee(targetName)) {
            executor.sendMessage(ChatColor.RED + "Already employed.");
            return true;
        }

        int executorGroup = company.getEmployeeGroup(executor.getName());
        int targetGroup = company.getGroupIdByName(roleName);

        if (targetGroup == -1 || executorGroup >= targetGroup) {
            executor.sendMessage(ChatColor.RED + "Invalid role.");
            return true;
        }

        company.addEmployee(targetName, targetGroup);
        company.executeGroupCommands("on-hire", targetName, targetGroup);

        executor.sendMessage(ChatColor.GREEN + "Player hired.");
        return true;
    }

    /* ========================== FIRE ========================== */

    private boolean handleFire(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player executor = (Player) sender;

        if (args.length < 3) {
            executor.sendMessage(ChatColor.RED +
                    "Usage: /company fire <player> <company>");
            return true;
        }

        String targetName = args[1];
        String companyName = args[2];

        Company company = manager.getCompany(companyName);

        if (company == null ||
                !company.hasPermission(executor.getName(), "can-fire") ||
                !company.isEmployee(executor.getName())) {
            executor.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (!company.isEmployee(targetName)) {
            executor.sendMessage(ChatColor.RED + "Not in company.");
            return true;
        }

        company.removeEmployee(targetName);
        company.executeGlobalCommands("on-fire", targetName);

        executor.sendMessage(ChatColor.GREEN + "Player fired.");
        return true;
    }

    /* ========================== LEAVE ========================== */

    private boolean handleLeave(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED +
                    "Usage: /company leave <company>");
            return true;
        }

        String companyName = args[1];
        Company company = manager.getCompany(companyName);

        if (company == null || !company.isEmployee(player.getName())) {
            player.sendMessage(ChatColor.RED + "You are not in this company.");
            return true;
        }

        company.removeEmployee(player.getName());
        company.executeGlobalCommands("on-fire", player.getName());

        player.sendMessage(ChatColor.RED + "You left the company.");
        return true;
    }

    /* ========================== DEPOSIT ========================== */

    private boolean handleDeposit(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED +
                    "Usage: /company deposit <company> <amount>");
            return true;
        }

        String companyName = args[1];
        Company company = manager.getCompany(companyName);

        if (company == null ||
                !company.hasPermission(player.getName(), "can-deposit")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
            return true;
        }

        if (plugin.getEconomy().getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "Not enough money.");
            return true;
        }

        plugin.getEconomy().withdrawPlayer(player, amount);
        company.deposit(amount);

        player.sendMessage(ChatColor.GREEN +
                "Deposited $" + amount + " to " + company.getDisplayName());

        return true;
    }

    /* ========================== WITHDRAW ========================== */

    private boolean handleWithdraw(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED +
                    "Usage: /company withdraw <company> <amount>");
            return true;
        }

        String companyName = args[1];
        Company company = manager.getCompany(companyName);

        if (company == null ||
                !company.hasPermission(player.getName(), "can-withdraw")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
            return true;
        }

        if (company.getBalance() < amount) {
            player.sendMessage(ChatColor.RED + "Not enough company funds.");
            return true;
        }

        company.withdraw(amount);
        plugin.getEconomy().depositPlayer(player, amount);

        player.sendMessage(ChatColor.GREEN +
                "Withdrew $" + amount + " from " + company.getDisplayName());

        return true;
    }

    /* ========================== RELOAD ========================== */

    private boolean handleReload(CommandSender sender) {

        if (!sender.hasPermission("company.reload")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        manager.reload();
        sender.sendMessage(ChatColor.GREEN + "CompanyEconomy reloaded.");
        return true;
    }

    /* ========================== TAB COMPLETE ========================== */

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        try {

            if (args.length == 1) {
                return filter(Arrays.asList(
                        "hire", "fire", "leave",
                        "deposit", "withdraw",
                        "reload", "info"
                ), args[0]);
            }

            if (!(sender instanceof Player))
                return Collections.emptyList();

            Player player = (Player) sender;
            String sub = args[0].toLowerCase();

            if (sub.equals("hire")) {

                if (args.length == 2)
                    return filter(getAllPlayerNames(), args[1]);

                if (args.length == 3)
                    return filter(getExecutorCompanies(player), args[2]);

                if (args.length == 4) {
                    Company company = manager.getCompany(args[2]);
                    if (company != null)
                        return filter(company.getGroupTags(), args[3]);
                }
            }

            if (sub.equals("fire")) {

                if (args.length == 2)
                    return filter(getAllPlayerNames(), args[1]);

                if (args.length == 3)
                    return filter(getExecutorCompanies(player), args[2]);
            }

            if (sub.equals("deposit") || sub.equals("withdraw")) {

                if (args.length == 2)
                    return filter(getExecutorCompanies(player), args[1]);
            }

            if (sub.equals("leave") && args.length == 2)
                return filter(getExecutorCompanies(player), args[1]);

            if (sub.equals("info") && args.length == 2)
                return filter(manager.getCompanyNames(), args[1]);

        } catch (Exception ignored) {}

        return Collections.emptyList();
    }

    /* ========================== HELPERS ========================== */

    private List<String> getExecutorCompanies(Player player) {
        return manager.getCompanies().stream()
                .filter(c -> c.isEmployee(player.getName()))
                .map(Company::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getAllPlayerNames() {
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> list, String current) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(current.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}
