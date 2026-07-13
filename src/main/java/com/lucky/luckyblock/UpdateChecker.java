package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin plugin;

    private final String projectSlug = "luckyengine"; // zmień na swój slug Modrinth

    private boolean updateAvailable = false;
    private String latestVersion = "";


    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public void check() {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            try {

                URL url = new URL(
                        "https://api.modrinth.com/v2/project/"
                                + projectSlug
                                + "/version"
                );


                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");

                connection.setRequestProperty(
                        "User-Agent",
                        "LuckyBlock/"
                                + plugin.getDescription().getVersion()
                );


                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );


                StringBuilder response = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();


                String json = response.toString();


                latestVersion =
                        json.split("\"version_number\":\"")[1]
                                .split("\"")[0];


                String currentVersion =
                        plugin.getDescription()
                                .getVersion();


                if (!currentVersion.equalsIgnoreCase(latestVersion)) {

                    updateAvailable = true;


                    Bukkit.getConsoleSender().sendMessage(
                            "§e[LuckyBlock] New update available!"
                    );

                    Bukkit.getConsoleSender().sendMessage(
                            "§7Current: §c" + currentVersion
                    );

                    Bukkit.getConsoleSender().sendMessage(
                            "§7Latest: §a" + latestVersion
                    );

                } else {

                    Bukkit.getConsoleSender().sendMessage(
                            "§a[LuckyBlock] You are running latest version."
                    );

                }


            } catch (Exception e) {

                Bukkit.getConsoleSender().sendMessage(
                        "§c[LuckyBlock] Update check failed."
                );

            }

        });

    }


    public void notifyPlayer(Player player) {

        if (!updateAvailable)
            return;


        if (!player.isOp()
                && !player.hasPermission("luckyblock.admin"))
            return;


        player.sendMessage(
                "§8§m----------------"
        );

        player.sendMessage(
                "§e[LuckyBlock] §fNew update available!"
        );

        player.sendMessage(
                "§7Current: §c"
                        + plugin.getDescription().getVersion()
        );

        player.sendMessage(
                "§7Latest: §a"
                        + latestVersion
        );

        player.sendMessage(
                "§7Download: §bmodrinth.com/plugin/"
                        + projectSlug
        );

        player.sendMessage(
                "§8§m----------------"
        );

    }
}