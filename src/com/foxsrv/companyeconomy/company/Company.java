package com.foxsrv.companyeconomy.company;

import com.foxsrv.companyeconomy.CompanyEconomy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Company {

    private final CompanyEconomy plugin;
    private final File file;
    private final YamlConfiguration config;

    private final String name;
    private final String displayName;

    private double balance;

    // PlayerName (lowercase) -> groupId
    private final Map<String, Integer> employees = new HashMap<>();

    public Company(CompanyEconomy plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        this.name = file.getName().replace(".yml", "");
        this.displayName = config.getString("displayName", name);
        this.balance = config.getDouble("balance", 0.0);

        loadEmployees();
    }

    /*
     * =========================
     *      LOAD EMPLOYEES
     * =========================
     */

    private void loadEmployees() {
        ConfigurationSection dataSection = config.getConfigurationSection("data");
        if (dataSection == null) return;

        for (String playerName : dataSection.getKeys(false)) {
            int group = dataSection.getInt(playerName + ".group");
            employees.put(playerName.toLowerCase(), group);
        }
    }

    /*
     * =========================
     *      BASIC GETTERS
     * =========================
     */

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBalance() {
        return balance;
    }

    public Map<String, Integer> getEmployees() {
        return employees;
    }

    public boolean isEmployee(String playerName) {
        if (playerName == null) return false;
        return employees.containsKey(playerName.toLowerCase());
    }

    public int getEmployeeGroup(String playerName) {
        if (playerName == null) return -1;
        return employees.getOrDefault(playerName.toLowerCase(), -1);
    }

    /*
     * =========================
     *      GROUP SYSTEM
     * =========================
     */

    public int getGroupIdByName(String roleName) {
        if (roleName == null) return -1;

        ConfigurationSection groups = config.getConfigurationSection("groups");
        if (groups == null) return -1;

        for (String id : groups.getKeys(false)) {
            ConfigurationSection section = groups.getConfigurationSection(id);
            if (section == null) continue;

            String tag = section.getString("tag");
            if (tag != null && tag.equalsIgnoreCase(roleName)) {
                return Integer.parseInt(id);
            }
        }

        return -1;
    }

    public List<String> getGroupTags() {
        List<String> tags = new ArrayList<>();

        ConfigurationSection groups = config.getConfigurationSection("groups");
        if (groups == null) return tags;

        for (String id : groups.getKeys(false)) {
            ConfigurationSection section = groups.getConfigurationSection(id);
            if (section == null) continue;

            String tag = section.getString("tag");
            if (tag != null)
                tags.add(tag);
        }

        return tags;
    }

    public double getSalary(int groupId) {
        return config.getDouble("groups." + groupId + ".salary", 0.0);
    }

    public boolean hasPermission(String playerName, String permission) {
        int group = getEmployeeGroup(playerName);
        if (group == -1) return false;

        return config.getBoolean(
                "groups." + group + ".permissions." + permission,
                false
        );
    }

    /*
     * =========================
     *      EMPLOYEE CONTROL
     * =========================
     */

    public void addEmployee(String playerName, int groupId) {

        if (playerName == null) return;

        String key = playerName.toLowerCase();

        employees.put(key, groupId);

        config.set("data." + playerName + ".group", groupId);

        save();
    }

    public void removeEmployee(String playerName) {

        if (playerName == null) return;

        String key = playerName.toLowerCase();

        employees.remove(key);

        config.set("data." + playerName, null);

        save();
    }

    /*
     * =========================
     *      BALANCE SYSTEM
     * =========================
     */

    public void deposit(double amount) {
        balance += amount;
        config.set("balance", balance);
        save();
    }

    public void withdraw(double amount) {
        balance -= amount;
        config.set("balance", balance);
        save();
    }

    /*
     * =========================
     *      COMMAND EXECUTION
     * =========================
     */

    // EXECUTA MESMO OFFLINE
    public void executeGroupCommands(String type, String playerName, int groupId) {

        List<String> commands = config.getStringList(
                "groups." + groupId + ".commands." + type
        );

        if (commands == null || commands.isEmpty()) return;

        for (String cmd : commands) {
            if (cmd == null || cmd.isEmpty()) continue;

            String parsed = cmd.replace("%player%", playerName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    // EXECUTA MESMO OFFLINE
    public void executeGlobalCommands(String type, String playerName) {

        List<String> commands = config.getStringList("commands." + type);

        if (commands == null || commands.isEmpty()) return;

        for (String cmd : commands) {
            if (cmd == null || cmd.isEmpty()) continue;

            String parsed = cmd.replace("%player%", playerName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    /*
     * =========================
     *          SAVE
     * =========================
     */

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save company file: " + name);
            e.printStackTrace();
        }
    }
}
