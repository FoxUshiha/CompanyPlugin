package com.foxsrv.companyeconomy.company;

import com.foxsrv.companyeconomy.CompanyEconomy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CompanyManager {

    private final CompanyEconomy plugin;
    private final Map<String, Company> companies = new HashMap<>();
    private File companiesFolder;

    public CompanyManager(CompanyEconomy plugin) {
        this.plugin = plugin;
        setupFolder();
    }

    /*
     * =========================
     *        FOLDER SETUP
     * =========================
     */

    private void setupFolder() {
        companiesFolder = new File(plugin.getDataFolder(), "companies");

        if (!companiesFolder.exists()) {
            if (companiesFolder.mkdirs()) {
                plugin.getLogger().info("Created companies folder.");
            }
        }
    }

    /*
     * =========================
     *   CREATE DEFAULT FILE
     * =========================
     */

    private void createDefaultCompanyIfMissing() {

        File defaultFile = new File(companiesFolder, "defaultCompany.yml");

        if (defaultFile.exists()) {
            return;
        }

        try {
            defaultFile.createNewFile();

            YamlConfiguration config = new YamlConfiguration();

            config.set("displayName", "Default Company");
            config.set("balance", 5000.0);

            // ===== GLOBAL COMMANDS =====
            config.set("commands.on-fire",
                    Collections.singletonList(
                            "say %player% has been fired!"
                    ));

            // ===== GROUP 1 (OWNER) =====
            config.set("groups.1.tag", "Owner");
            config.set("groups.1.salary", 300.0);
            config.set("groups.1.permissions.can-hire", true);
            config.set("groups.1.permissions.can-fire", true);
            config.set("groups.1.permissions.can-deposit", true);
            config.set("groups.1.permissions.can-withdraw", true);
            config.set("groups.1.commands.on-hire",
                    Collections.singletonList(
                            "say %player% is now the owner!"
                    ));

            // ===== CONTRACT =====
            config.set("contract.enabled", true);
            config.set("contract.auto-send-on-hire", true);
            config.set("contract.lines", Arrays.asList(
                    "&6Employment Contract - Default Company",
                    "&7--------------------------------------",
                    "&7You agree to follow company rules.",
                    "&7Breaking rules may result in termination.",
                    "&aSalary will be paid every 30 minutes.",
                    "&7--------------------------------------"
            ));

            // ===== DEFAULT EMPLOYEE (NAME SYSTEM) =====
            config.set("data.Steve.group", 1);

            config.save(defaultFile);

            plugin.getLogger().info(
                    "defaultCompany.yml created with Steve as default owner."
            );

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create defaultCompany.yml");
            e.printStackTrace();
        }
    }

    /*
     * =========================
     *       LOAD SYSTEM
     * =========================
     */

    public void loadCompanies() {

        companies.clear();

        createDefaultCompanyIfMissing();

        File[] files = companiesFolder.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".yml")
        );

        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No company files found.");
            return;
        }

        for (File file : files) {
            try {
                Company company = new Company(plugin, file);
                companies.put(company.getName().toLowerCase(), company);

                plugin.getLogger().info("Loaded company: " + company.getName());

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load company file: "
                        + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Total companies loaded: " + companies.size());
    }

    /*
     * =========================
     *        RELOAD
     * =========================
     */

    public void reload() {
        loadCompanies();
    }

    /*
     * =========================
     *        GETTERS
     * =========================
     */

    public Company getCompany(String name) {
        if (name == null) return null;
        return companies.get(name.toLowerCase());
    }

    public List<Company> getCompanies() {
        return new ArrayList<>(companies.values());
    }

    public List<String> getCompanyNames() {
        return companies.values().stream()
                .map(Company::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    /*
     * =========================
     *  COMPANY RESOLUTION
     * =========================
     */

    public Company resolveCompanyForExecutor(String executorName,
                                             String companyArg,
                                             String permission) {

        if (companyArg != null) {
            Company company = getCompany(companyArg);
            if (company != null &&
                    company.hasPermission(executorName, permission)) {
                return company;
            }
            return null;
        }

        return companies.values().stream()
                .sorted(Comparator.comparing(Company::getName))
                .filter(c -> c.hasPermission(executorName, permission))
                .findFirst()
                .orElse(null);
    }

    /*
     * =========================
     *   OPTIONAL: CREATE NEW
     * =========================
     */

    public Company createCompany(String name) {

        String fileName = name + ".yml";
        File file = new File(companiesFolder, fileName);

        if (file.exists()) {
            return null;
        }

        try {
            if (file.createNewFile()) {

                YamlConfiguration config = new YamlConfiguration();

                config.set("displayName", name);
                config.set("balance", 0.0);
                config.save(file);

                Company company = new Company(plugin, file);
                companies.put(name.toLowerCase(), company);
                return company;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not create company: " + name);
            e.printStackTrace();
        }

        return null;
    }

    /*
     * =========================
     *    OPTIONAL: DELETE
     * =========================
     */

    public boolean deleteCompany(String name) {

        Company company = getCompany(name);
        if (company == null) return false;

        File file = new File(companiesFolder,
                company.getName() + ".yml");

        companies.remove(name.toLowerCase());

        return file.delete();
    }
}
