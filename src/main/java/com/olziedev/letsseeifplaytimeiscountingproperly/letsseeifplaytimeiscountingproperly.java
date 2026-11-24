package com.olziedev.letsseeifplaytimeiscountingproperly;

import com.olziedev.spotextras.SpotExtras;
import com.olziedev.spotextras.api.SpotPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class letsseeifplaytimeiscountingproperly extends SpotPlugin
        implements Listener, CommandExecutor, TabCompleter {

    private static final String PERMISSION = "perchextras.playtimeaudit";

    private static final String PH_TOTAL_SECONDS = "%jetsantiafkpro_timeplayed_seconds_unformatted%";
    private static final String PH_AFK_SECONDS = "%jetsantiafkpro_afk_seconds%";
    private static final String PH_IS_AFK = "%jetsantiafkpro_isafk%";
    private static final String PH_SESSION_DAYS = "%jetsantiafkpro_session_timeplayed_days%";
    private static final String PH_SESSION_HOURS = "%jetsantiafkpro_session_timeplayed_hours%";
    private static final String PH_SESSION_MINUTES = "%jetsantiafkpro_session_timeplayed_minutes%";
    private static final String PH_SESSION_SECONDS = "%jetsantiafkpro_session_timeplayed_seconds%";

    private static final long JOIN_DELAY_TICKS = 100L;          // 5s delay so Jets finishes async DB load
    private static final long JOIN_VERIFY_DELAY_TICKS = 200L;   // +10s follow-up if JOIN looks anomalous
    private static final long SAMPLE_INTERVAL_TICKS = 6000L;    // Every 5 minutes (matches Jets auto-save)
    private static final long STALL_MIN_VANILLA_DELTA = 240L;   // 4+ mins vanilla gain with 0 Jets gain
    private static final int STALL_CONSECUTIVE_REQUIRED = 2;    // 2 windows (10 mins) to prevent AFK false positives
    private static final long ROLLBACK_MATCH_TOLERANCE_SEC = 15L;

    private static final DateTimeFormatter CHAT_TIME_FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter CSV_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FILE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    private final Map<UUID, PlayerState> lastKnown = new ConcurrentHashMap<>();
    private final Set<UUID> readyPlayers = ConcurrentHashMap.newKeySet();

    private String bootId;
    private volatile boolean shuttingDown = false;
    private File dbFile;
    private Connection connection;
    private ExecutorService dbExecutor;
    private BukkitTask periodicTask;
    private boolean papiAvailable;

    @Override
    public String getName() {
        return "PlaytimeAudit";
    }

    @Override
    public void onEnable() {
        this.shuttingDown = false;
        this.bootId = FILE_TIME_FMT.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 4);

        this.papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!this.papiAvailable) {
            this.plugin.getLogger().warning("[PlaytimeAudit] PlaceholderAPI not found! Playtime tracking disabled.");
            return;
        }

        if (!initDatabase()) {
            this.plugin.getLogger().severe("[PlaytimeAudit] Failed to initialize SQLite database. Module disabled.");
            return;
        }

        loadLastKnownStates();

        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerchExtras-PlaytimeAudit-SQL");
            t.setDaemon(true);
            return t;
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            readyPlayers.add(online.getUniqueId());
        }

        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        PluginCommand cmd = SpotExtras.getInstance().getCommand("playtimeaudit");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        } else {
            this.plugin.getLogger().warning("[PlaytimeAudit] Command 'playtimeaudit' not found in plugin.yml!");
        }

        this.periodicTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (readyPlayers.contains(player.getUniqueId())) {
                    captureAndEnqueue(player, "PERIODIC", true, false);
                }
            }
        }, SAMPLE_INTERVAL_TICKS, SAMPLE_INTERVAL_TICKS);

        this.plugin.getLogger().info("[PlaytimeAudit] SQLite audit tracker enabled (boot=" + bootId + ", tracked=" + lastKnown.size() + ").");
    }

    @Override
    public void onDisable() {
        this.shuttingDown = true;

        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }

        if (papiAvailable && dbExecutor != null && !dbExecutor.isShutdown()) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (readyPlayers.contains(player.getUniqueId())) {
                        captureAndEnqueue(player, "SHUTDOWN", false, false);
                    }
                }
            }

            dbExecutor.shutdown();
            try {
                if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.plugin.getLogger().warning("[PlaytimeAudit] Timed out waiting for SQL writes to finish.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        readyPlayers.clear();

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.WARNING, "[PlaytimeAudit] Error closing SQLite connection", e);
            }
            connection = null;
        }
    }

    // -------------------------------------------------------------------------
    // Commands & Tab Completion
    // -------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (!papiAvailable || dbExecutor == null || dbExecutor.isShutdown()) {
            sender.sendMessage(Component.text("PlaytimeAudit is currently not active.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "anomalies", "a" -> {
                int limit = (args.length >= 2) ? parseLimit(args[1], 10, 50) : 10;
                handleAnomalies(sender, limit);
            }
            case "patterns", "p", "report" -> handlePatterns(sender);
            case "check", "c", "history" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " check <player> [limit]", NamedTextColor.RED));
                    return true;
                }
                int limit = (args.length >= 3) ? parseLimit(args[2], 10, 50) : 10;
                handleCheck(sender, args[1], limit);
            }
            case "live", "l" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " live <player>", NamedTextColor.RED));
                    return true;
                }
                handleLive(sender, args[1]);
            }
            case "stats", "s" -> handleStats(sender);
            case "export", "csv" -> {
                String target = (args.length >= 2) ? args[1] : "all";
                handleExport(sender, target);
            }
            default -> sendUsage(sender, label);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(List.of("patterns", "anomalies", "check", "live", "stats", "export"), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "check", "c", "history", "live", "l" -> filterPrefix(onlinePlayerNames(), args[1]);
                case "anomalies", "a" -> filterPrefix(List.of("10", "20", "50"), args[1]);
                case "export", "csv" -> {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    options.add("anomalies");
                    options.addAll(onlinePlayerNames());
                    yield filterPrefix(options, args[1]);
                }
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3 && (sub.equals("check") || sub.equals("c") || sub.equals("history"))) {
            return filterPrefix(List.of("10", "20", "50"), args[2]);
        }

        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== PlaytimeAudit Commands ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/" + label + " patterns", NamedTextColor.YELLOW)
                .append(Component.text(" - Rollback vs subtraction & timing pattern report", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " anomalies [limit]", NamedTextColor.YELLOW)
                .append(Component.text(" - Recent drops, rollbacks, and mismatches", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " check <player> [limit]", NamedTextColor.YELLOW)
                .append(Component.text(" - Player sample timeline", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " live <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Instant Jets-to-Jets & vanilla readout", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " stats", NamedTextColor.YELLOW)
                .append(Component.text(" - Database totals and anomaly counts", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " export [all|anomalies|<player>]", NamedTextColor.YELLOW)
                .append(Component.text(" - Export Excel-ready CSV files", NamedTextColor.GRAY)));
    }

    private void handlePatterns(CommandSender sender) {
        dbExecutor.submit(() -> {
            if (!isConnectionOpen()) return;

            List<Component> out = new ArrayList<>();
            out.add(Component.text("=== Playtime Loss Pattern Analysis ===", NamedTextColor.GOLD, TextDecoration.BOLD));

            try (Statement st = connection.createStatement()) {
                long totalAnomalies = 0;
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM playtime_anomalies")) {
                    if (rs.next()) totalAnomalies = rs.getLong(1);
                }

                if (totalAnomalies == 0) {
                    out.add(Component.text("No anomalies recorded yet. Nothing abnormal detected.", NamedTextColor.GREEN));
                    sendSync(sender, out);
                    return;
                }

                // 1. Classification breakdown (Rollback vs Subtraction vs Wipe vs Mismatch)
                out.add(Component.text("1) By Type:", NamedTextColor.YELLOW, TextDecoration.BOLD));
                try (ResultSet rs = st.executeQuery("""
                        SELECT anomaly_type, COUNT(*) AS cnt, COALESCE(SUM(loss_seconds), 0) AS total_loss
                        FROM playtime_anomalies
                        GROUP BY anomaly_type
                        ORDER BY cnt DESC
                        """)) {
                    while (rs.next()) {
                        String type = rs.getString("anomaly_type");
                        long cnt = rs.getLong("cnt");
                        long loss = rs.getLong("total_loss");
                        String lossText = loss > 0 ? (" | total lost: " + formatDuration(loss)) : "";
                        out.add(Component.text("  " + type + ": ", colorForAnomaly(type))
                                .append(Component.text(cnt + "x" + lossText, NamedTextColor.WHITE)));
                    }
                }

                // 2. Trigger & Restart correlation
                StringBuilder triggerSummary = new StringBuilder();
                try (ResultSet rs = st.executeQuery("""
                        SELECT trigger_type, COUNT(*) AS cnt
                        FROM playtime_anomalies
                        GROUP BY trigger_type
                        ORDER BY cnt DESC
                        """)) {
                    while (rs.next()) {
                        if (!triggerSummary.isEmpty()) triggerSummary.append(", ");
                        triggerSummary.append(rs.getString("trigger_type")).append("=").append(rs.getLong("cnt"));
                    }
                }

                long acrossRestart = 0;
                long sameUptime = 0;
                try (ResultSet rs = st.executeQuery("""
                        SELECT
                            COALESCE(SUM(CASE WHEN crossed_restart = 1 THEN 1 ELSE 0 END), 0) AS across,
                            COALESCE(SUM(CASE WHEN crossed_restart = 0 THEN 1 ELSE 0 END), 0) AS same
                        FROM playtime_anomalies
                        """)) {
                    if (rs.next()) {
                        acrossRestart = rs.getLong("across");
                        sameUptime = rs.getLong("same");
                    }
                }

                out.add(Component.text("2) When It Happens:", NamedTextColor.YELLOW, TextDecoration.BOLD));
                out.add(Component.text("  Triggers: ", NamedTextColor.GRAY)
                        .append(Component.text(triggerSummary.toString(), NamedTextColor.WHITE)));
                out.add(Component.text("  Restart link: ", NamedTextColor.GRAY)
                        .append(Component.text(acrossRestart + " across restart", NamedTextColor.AQUA))
                        .append(Component.text(" vs ", NamedTextColor.GRAY))
                        .append(Component.text(sameUptime + " same uptime", NamedTextColor.WHITE)));

                // 3. Loss & Rollback Age stats
                try (ResultSet rs = st.executeQuery("""
                        SELECT
                            COUNT(loss_seconds) AS drop_cnt,
                            AVG(loss_seconds) AS avg_loss,
                            MAX(loss_seconds) AS max_loss,
                            COUNT(rollback_age_seconds) AS rb_cnt,
                            AVG(rollback_age_seconds) AS avg_rb_age
                        FROM playtime_anomalies
                        WHERE loss_seconds > 0
                        """)) {
                    if (rs.next() && rs.getLong("drop_cnt") > 0) {
                        long avgLoss = Math.round(rs.getDouble("avg_loss"));
                        long maxLoss = rs.getLong("max_loss");
                        long rbCnt = rs.getLong("rb_cnt");
                        long avgRbAge = Math.round(rs.getDouble("avg_rb_age"));
                        out.add(Component.text("  Drop size: ", NamedTextColor.GRAY)
                                .append(Component.text("avg -" + formatDuration(avgLoss) + ", max -" + formatDuration(maxLoss), NamedTextColor.RED)));
                        if (rbCnt > 0) {
                            out.add(Component.text("  Rollback jump: ", NamedTextColor.GRAY)
                                    .append(Component.text(rbCnt + " matched past timestamps (avg " + formatDuration(avgRbAge) + " back)", NamedTextColor.AQUA)));
                        }
                    }
                }

                // 4. Hour-of-day clustering (top 3 hours)
                StringBuilder hourText = new StringBuilder();
                try (ResultSet rs = st.executeQuery("""
                        SELECT strftime('%H', recorded_at / 1000, 'unixepoch', 'localtime') AS hr, COUNT(*) AS cnt
                        FROM playtime_anomalies
                        GROUP BY hr
                        ORDER BY cnt DESC
                        LIMIT 3
                        """)) {
                    while (rs.next()) {
                        if (!hourText.isEmpty()) hourText.append(", ");
                        hourText.append(rs.getString("hr")).append(":00 (").append(rs.getLong("cnt")).append("x)");
                    }
                }
                out.add(Component.text("3) Time Hotspots (Local): ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .append(Component.text(hourText.toString(), NamedTextColor.WHITE)));

                // 5. Simultaneous multi-player waves (2+ distinct players within same minute)
                List<String> waves = new ArrayList<>();
                try (ResultSet rs = st.executeQuery("""
                        SELECT MIN(recorded_at) AS ts, COUNT(DISTINCT uuid) AS players, COUNT(*) AS cnt
                        FROM playtime_anomalies
                        WHERE loss_seconds > 0
                        GROUP BY (recorded_at / 60000)
                        HAVING COUNT(DISTINCT uuid) >= 2
                        ORDER BY ts DESC
                        LIMIT 3
                        """)) {
                    while (rs.next()) {
                        String t = CHAT_TIME_FMT.format(Instant.ofEpochMilli(rs.getLong("ts")));
                        waves.add(t + " (" + rs.getLong("players") + " players)");
                    }
                }
                if (waves.isEmpty()) {
                    out.add(Component.text("4) Multi-Player Waves: ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                            .append(Component.text("None (all drops isolated to single players)", NamedTextColor.GRAY)));
                } else {
                    out.add(Component.text("4) Multi-Player Waves: ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text(String.join(", ", waves), NamedTextColor.WHITE)));
                }

                // 6. Top affected players
                StringBuilder topPlayers = new StringBuilder();
                try (ResultSet rs = st.executeQuery("""
                        SELECT player_name, COUNT(*) AS cnt, COALESCE(SUM(loss_seconds), 0) AS total_loss
                        FROM playtime_anomalies
                        GROUP BY uuid
                        ORDER BY total_loss DESC, cnt DESC
                        LIMIT 5
                        """)) {
                    while (rs.next()) {
                        if (!topPlayers.isEmpty()) topPlayers.append(", ");
                        topPlayers.append(rs.getString("player_name"))
                                .append(" (").append(rs.getLong("cnt")).append("x");
                        long loss = rs.getLong("total_loss");
                        if (loss > 0) {
                            topPlayers.append(", -").append(formatDuration(loss));
                        }
                        topPlayers.append(")");
                    }
                }
                out.add(Component.text("5) Most Affected: ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .append(Component.text(topPlayers.toString(), NamedTextColor.WHITE)));

            } catch (SQLException e) {
                sendSync(sender, List.of(Component.text("Database error: " + e.getMessage(), NamedTextColor.RED)));
                return;
            }

            sendSync(sender, out);
        });
    }

    private void handleAnomalies(CommandSender sender, int limit) {
        dbExecutor.submit(() -> {
            if (!isConnectionOpen()) return;
            List<Component> lines = new ArrayList<>();
            String sql = """
                    SELECT recorded_at, crossed_restart, player_name, trigger_type, anomaly_type,
                           previous_seconds, current_seconds, loss_seconds, rollback_age_seconds, details
                    FROM playtime_anomalies
                    ORDER BY recorded_at DESC
                    LIMIT ?
                    """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String time = CHAT_TIME_FMT.format(Instant.ofEpochMilli(rs.getLong("recorded_at")));
                        boolean crossedRestart = rs.getInt("crossed_restart") == 1;
                        String name = rs.getString("player_name");
                        String trigger = rs.getString("trigger_type");
                        String type = rs.getString("anomaly_type");
                        Long prev = getNullableLong(rs, "previous_seconds");
                        Long curr = getNullableLong(rs, "current_seconds");
                        Long loss = getNullableLong(rs, "loss_seconds");
                        Long rbAge = getNullableLong(rs, "rollback_age_seconds");
                        String details = rs.getString("details");

                        Component row = Component.text("[" + time + "] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(type, colorForAnomaly(type), TextDecoration.BOLD))
                                .append(Component.text(" " + name + " ", NamedTextColor.WHITE))
                                .append(Component.text("(" + trigger + (crossedRestart ? "+RESTART" : "") + ") ", NamedTextColor.GRAY));

                        if (prev != null && curr != null && loss != null && loss > 0) {
                            row = row.append(Component.text(formatDuration(prev) + " -> " + formatDuration(curr), NamedTextColor.AQUA))
                                    .append(Component.text(" (-" + formatDuration(loss) + ")", NamedTextColor.RED));
                            if (rbAge != null) {
                                row = row.append(Component.text(" [rolled back " + formatDuration(rbAge) + "]", NamedTextColor.GOLD));
                            }
                        } else if (details != null) {
                            row = row.append(Component.text(details, NamedTextColor.GRAY));
                        }

                        lines.add(row);
                    }
                }
            } catch (SQLException e) {
                sendSync(sender, List.of(Component.text("Database error: " + e.getMessage(), NamedTextColor.RED)));
                return;
            }

            if (lines.isEmpty()) {
                sendSync(sender, List.of(Component.text("No anomalies recorded yet.", NamedTextColor.GREEN)));
                return;
            }

            Collections.reverse(lines);
            List<Component> out = new ArrayList<>();
            out.add(Component.text("=== Latest " + lines.size() + " Playtime Anomalies ===", NamedTextColor.GOLD, TextDecoration.BOLD));
            out.addAll(lines);
            sendSync(sender, out);
        });
    }

    private void handleCheck(CommandSender sender, String targetName, int limit) {
        dbExecutor.submit(() -> {
            if (!isConnectionOpen()) return;
            List<Component> rows = new ArrayList<>();
            String resolvedName = targetName;
            int anomalyCount = 0;
            Long peakJets = null;

            String sampleSql = """
                    SELECT recorded_at, player_name, trigger_type, jets_seconds, jets_raw,
                           jets_session_seconds, jets_afk_seconds, vanilla_seconds,
                           delta_jets, delta_session, delta_vanilla
                    FROM playtime_samples
                    WHERE LOWER(player_name) = LOWER(?)
                    ORDER BY recorded_at DESC
                    LIMIT ?
                    """;

            String anomalyCountSql = "SELECT COUNT(*) FROM playtime_anomalies WHERE LOWER(player_name) = LOWER(?)";
            String peakSql = "SELECT peak_jets_seconds FROM playtime_last_known WHERE LOWER(player_name) = LOWER(?) LIMIT 1";

            try (PreparedStatement ps = connection.prepareStatement(sampleSql)) {
                ps.setString(1, targetName);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        resolvedName = rs.getString("player_name");
                        String time = CHAT_TIME_FMT.format(Instant.ofEpochMilli(rs.getLong("recorded_at")));
                        String trigger = rs.getString("trigger_type");
                        Long jets = getNullableLong(rs, "jets_seconds");
                        String raw = rs.getString("jets_raw");
                        Long sess = getNullableLong(rs, "jets_session_seconds");
                        Long afk = getNullableLong(rs, "jets_afk_seconds");
                        long vanilla = rs.getLong("vanilla_seconds");
                        Long dJets = getNullableLong(rs, "delta_jets");
                        Long dVanilla = getNullableLong(rs, "delta_vanilla");

                        NamedTextColor deltaColor = (dJets != null && dJets < 0) ? NamedTextColor.RED
                                : (dJets != null && dJets == 0 && dVanilla != null && dVanilla >= STALL_MIN_VANILLA_DELTA)
                                ? NamedTextColor.YELLOW
                                : NamedTextColor.GREEN;

                        String jetsText = (jets != null) ? formatDuration(jets) : ("ERR(" + raw + ")");

                        Component line = Component.text("[" + time + "] ", NamedTextColor.DARK_GRAY)
                                .append(Component.text(String.format("%-11s", trigger) + " ", NamedTextColor.GRAY))
                                .append(Component.text("Tot: ", NamedTextColor.DARK_AQUA))
                                .append(Component.text(jetsText, jets != null ? NamedTextColor.AQUA : NamedTextColor.RED))
                                .append(Component.text(" (" + formatSignedDelta(dJets) + ")", deltaColor));

                        if (sess != null) {
                            line = line.append(Component.text(" | Sess: " + formatDuration(sess), NamedTextColor.LIGHT_PURPLE));
                        }

                        line = line.append(Component.text(" | Van: " + formatDuration(vanilla) + " (" + formatSignedDelta(dVanilla) + ")", NamedTextColor.GRAY));

                        if (afk != null && afk > 0) {
                            line = line.append(Component.text(" [AFK " + formatDuration(afk) + "]", NamedTextColor.GOLD));
                        }

                        rows.add(line);
                    }
                }

                try (PreparedStatement aps = connection.prepareStatement(anomalyCountSql)) {
                    aps.setString(1, targetName);
                    try (ResultSet ars = aps.executeQuery()) {
                        if (ars.next()) anomalyCount = ars.getInt(1);
                    }
                }

                try (PreparedStatement pps = connection.prepareStatement(peakSql)) {
                    pps.setString(1, targetName);
                    try (ResultSet prs = pps.executeQuery()) {
                        if (prs.next()) peakJets = getNullableLong(prs, "peak_jets_seconds");
                    }
                }
            } catch (SQLException e) {
                sendSync(sender, List.of(Component.text("Database error: " + e.getMessage(), NamedTextColor.RED)));
                return;
            }

            if (rows.isEmpty()) {
                sendSync(sender, List.of(Component.text("No samples found for '" + targetName + "'.", NamedTextColor.YELLOW)));
                return;
            }

            Collections.reverse(rows);
            List<Component> out = new ArrayList<>();
            Component header = Component.text("=== History: " + resolvedName + " ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text("(anomalies: " + anomalyCount,
                            anomalyCount > 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
            if (peakJets != null) {
                header = header.append(Component.text(", peak: " + formatDuration(peakJets), NamedTextColor.AQUA));
            }
            header = header.append(Component.text(")", NamedTextColor.GRAY));
            out.add(header);
            out.addAll(rows);
            sendSync(sender, out);
        });
    }

    private void handleLive(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + targetName + "' is not online. Use /playtimeaudit check " + targetName, NamedTextColor.YELLOW));
            return;
        }

        String rawJets = PlaceholderAPI.setPlaceholders(target, PH_TOTAL_SECONDS);
        String rawAfkSec = PlaceholderAPI.setPlaceholders(target, PH_AFK_SECONDS);
        String rawIsAfk = PlaceholderAPI.setPlaceholders(target, PH_IS_AFK);
        Long jetsSec = parseLongOrNull(rawJets);
        Long sessionSec = readSessionSeconds(target);
        Long afkSec = parseLongOrNull(rawAfkSec);
        long vanillaSec = target.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        PlayerState prev = lastKnown.get(target.getUniqueId());

        sender.sendMessage(Component.text("=== Live Readout: " + target.getName() + " ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Jets Total: ", NamedTextColor.GRAY)
                .append(Component.text(jetsSec != null ? formatDuration(jetsSec) + " (" + jetsSec + "s)" : "PARSE ERROR: " + rawJets,
                        jetsSec != null ? NamedTextColor.AQUA : NamedTextColor.RED)));
        sender.sendMessage(Component.text("Jets Session: ", NamedTextColor.GRAY)
                .append(Component.text(sessionSec != null ? formatDuration(sessionSec) + " (" + sessionSec + "s)" : "unparsed", NamedTextColor.LIGHT_PURPLE)));
        sender.sendMessage(Component.text("Vanilla Total: ", NamedTextColor.GRAY)
                .append(Component.text(formatDuration(vanillaSec) + " (" + vanillaSec + "s)", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("AFK Status: ", NamedTextColor.GRAY)
                .append(Component.text("isAfk='" + rawIsAfk + "' | afkSeconds=" + (afkSec != null ? afkSec : rawAfkSec), NamedTextColor.YELLOW)));

        if (prev != null && prev.jetsSeconds != null) {
            String ago = formatDuration((System.currentTimeMillis() - prev.recordedAt) / 1000L);
            Long dJets = (jetsSec != null) ? (jetsSec - prev.jetsSeconds) : null;
            long dVan = vanillaSec - prev.vanillaSeconds;
            sender.sendMessage(Component.text("Since last sample (" + ago + " ago): ", NamedTextColor.GRAY)
                    .append(Component.text("Jets " + formatSignedDelta(dJets),
                            (dJets != null && dJets < 0) ? NamedTextColor.RED : NamedTextColor.GREEN))
                    .append(Component.text(" | Vanilla " + formatSignedDelta(dVan), NamedTextColor.GRAY)));

            if (prev.peakJetsSeconds != null && jetsSec != null && jetsSec < prev.peakJetsSeconds) {
                long belowPeak = prev.peakJetsSeconds - jetsSec;
                sender.sendMessage(Component.text("WARNING: Currently ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text(formatDuration(belowPeak) + " below recorded peak (" + formatDuration(prev.peakJetsSeconds) + ")!", NamedTextColor.RED)));
            }
        }
    }

    private void handleStats(CommandSender sender) {
        dbExecutor.submit(() -> {
            if (!isConnectionOpen()) return;
            long totalSamples = 0;
            long trackedPlayers = 0;
            long totalAnomalies = 0;
            Map<String, Long> byType = new LinkedHashMap<>();

            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM playtime_samples")) {
                    if (rs.next()) totalSamples = rs.getLong(1);
                }
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM playtime_last_known")) {
                    if (rs.next()) trackedPlayers = rs.getLong(1);
                }
                try (ResultSet rs = st.executeQuery("SELECT anomaly_type, COUNT(*) FROM playtime_anomalies GROUP BY anomaly_type")) {
                    while (rs.next()) {
                        long count = rs.getLong(2);
                        byType.put(rs.getString(1), count);
                        totalAnomalies += count;
                    }
                }
            } catch (SQLException e) {
                sendSync(sender, List.of(Component.text("Database error: " + e.getMessage(), NamedTextColor.RED)));
                return;
            }

            String sizeText = (dbFile != null && dbFile.exists())
                    ? String.format(Locale.US, "%.2f MB", dbFile.length() / (1024.0 * 1024.0))
                    : "unknown";

            List<Component> out = new ArrayList<>();
            out.add(Component.text("=== PlaytimeAudit Stats ===", NamedTextColor.GOLD, TextDecoration.BOLD));
            out.add(Component.text("Boot ID: ", NamedTextColor.GRAY).append(Component.text(bootId, NamedTextColor.WHITE)));
            out.add(Component.text("Tracked Players: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(trackedPlayers), NamedTextColor.WHITE)));
            out.add(Component.text("Total Samples: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(totalSamples), NamedTextColor.WHITE)));
            out.add(Component.text("Total Anomalies: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(totalAnomalies), totalAnomalies > 0 ? NamedTextColor.RED : NamedTextColor.GREEN)));
            for (Map.Entry<String, Long> entry : byType.entrySet()) {
                out.add(Component.text("  - " + entry.getKey() + ": ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(entry.getValue()), colorForAnomaly(entry.getKey()))));
            }
            out.add(Component.text("SQLite Size: ", NamedTextColor.GRAY).append(Component.text(sizeText, NamedTextColor.AQUA)));

            sendSync(sender, out);
        });
    }

    private void handleExport(CommandSender sender, String target) {
        sender.sendMessage(Component.text("Exporting CSV(s) asynchronously...", NamedTextColor.GRAY));

        dbExecutor.submit(() -> {
            if (!isConnectionOpen()) return;
            File exportDir = new File(this.plugin.getDataFolder(), "playtime-exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                sendSync(sender, List.of(Component.text("Could not create export directory.", NamedTextColor.RED)));
                return;
            }

            String stamp = FILE_TIME_FMT.format(Instant.now());
            List<Component> out = new ArrayList<>();

            try {
                if (target.equalsIgnoreCase("all")) {
                    File anomFile = new File(exportDir, "anomalies_" + stamp + ".csv");
                    int anomRows = exportAnomaliesCsv(anomFile);

                    File samplesFile = new File(exportDir, "samples_all_" + stamp + ".csv");
                    int sampleRows = exportSamplesCsv(samplesFile, null);

                    out.add(Component.text("Exported " + anomRows + " anomalies -> " + anomFile.getName(), NamedTextColor.GREEN));
                    out.add(Component.text("Exported " + sampleRows + " samples -> " + samplesFile.getName(), NamedTextColor.GREEN));
                } else if (target.equalsIgnoreCase("anomalies")) {
                    File anomFile = new File(exportDir, "anomalies_" + stamp + ".csv");
                    int anomRows = exportAnomaliesCsv(anomFile);
                    out.add(Component.text("Exported " + anomRows + " anomalies -> " + anomFile.getName(), NamedTextColor.GREEN));
                } else {
                    String safeName = target.replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    File playerFile = new File(exportDir, "samples_" + safeName + "_" + stamp + ".csv");
                    int rows = exportSamplesCsv(playerFile, target);
                    out.add(Component.text("Exported " + rows + " samples for " + target + " -> " + playerFile.getName(), NamedTextColor.GREEN));
                }
                out.add(Component.text("Folder: plugins/PerchExtras/playtime-exports/", NamedTextColor.GRAY));
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "[PlaytimeAudit] CSV export failed", e);
                out.add(Component.text("Export failed: " + e.getMessage(), NamedTextColor.RED));
            }

            sendSync(sender, out);
        });
    }

    private int exportAnomaliesCsv(File file) throws IOException, SQLException {
        String sql = """
                SELECT recorded_at, boot_id, crossed_restart, uuid, player_name, trigger_type, anomaly_type,
                       previous_seconds, current_seconds, peak_seconds, loss_seconds,
                       matched_past_at, rollback_age_seconds, delta_vanilla, details
                FROM playtime_anomalies
                ORDER BY recorded_at ASC
                """;

        int count = 0;
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            w.write('\uFEFF');
            w.write("timestamp,boot_id,crossed_restart,uuid,player_name,trigger,anomaly_type,previous_fmt,current_fmt,peak_fmt,loss_fmt,matched_past_time,rollback_age_fmt,previous_sec,current_sec,peak_sec,loss_sec,rollback_age_sec,delta_vanilla_sec,details\n");

            while (rs.next()) {
                String ts = CSV_TIME_FMT.format(Instant.ofEpochMilli(rs.getLong("recorded_at")));
                Long prev = getNullableLong(rs, "previous_seconds");
                Long curr = getNullableLong(rs, "current_seconds");
                Long peak = getNullableLong(rs, "peak_seconds");
                Long loss = getNullableLong(rs, "loss_seconds");
                Long matchedAt = getNullableLong(rs, "matched_past_at");
                Long rbAge = getNullableLong(rs, "rollback_age_seconds");
                Long dVan = getNullableLong(rs, "delta_vanilla");

                w.write(String.join(",",
                        csv(ts),
                        csv(rs.getString("boot_id")),
                        csv(rs.getInt("crossed_restart") == 1 ? "YES" : "NO"),
                        csv(rs.getString("uuid")),
                        csv(rs.getString("player_name")),
                        csv(rs.getString("trigger_type")),
                        csv(rs.getString("anomaly_type")),
                        csv(prev != null ? formatDuration(prev) : ""),
                        csv(curr != null ? formatDuration(curr) : ""),
                        csv(peak != null ? formatDuration(peak) : ""),
                        csv(loss != null ? formatDuration(loss) : ""),
                        csv(matchedAt != null ? CSV_TIME_FMT.format(Instant.ofEpochMilli(matchedAt)) : ""),
                        csv(rbAge != null ? formatDuration(rbAge) : ""),
                        csv(prev),
                        csv(curr),
                        csv(peak),
                        csv(loss),
                        csv(rbAge),
                        csv(dVan),
                        csv(rs.getString("details"))
                ));
                w.write('\n');
                count++;
            }
        }
        return count;
    }

    private int exportSamplesCsv(File file, String playerFilter) throws IOException, SQLException {
        String sql = (playerFilter == null)
                ? """
                  SELECT recorded_at, boot_id, uuid, player_name, trigger_type, jets_seconds, jets_raw,
                         jets_session_seconds, jets_afk_seconds, jets_is_afk_raw, vanilla_seconds,
                         delta_jets, delta_session, delta_vanilla, elapsed_seconds, world
                  FROM playtime_samples
                  ORDER BY recorded_at ASC
                  """
                : """
                  SELECT recorded_at, boot_id, uuid, player_name, trigger_type, jets_seconds, jets_raw,
                         jets_session_seconds, jets_afk_seconds, jets_is_afk_raw, vanilla_seconds,
                         delta_jets, delta_session, delta_vanilla, elapsed_seconds, world
                  FROM playtime_samples
                  WHERE LOWER(player_name) = LOWER(?)
                  ORDER BY recorded_at ASC
                  """;

        int count = 0;
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (playerFilter != null) {
                ps.setString(1, playerFilter);
            }

            w.write('\uFEFF');
            w.write("timestamp,boot_id,uuid,player_name,trigger,jets_fmt,session_fmt,vanilla_fmt,jets_sec,jets_raw,session_sec,afk_sec,is_afk_raw,vanilla_sec,delta_jets_sec,delta_session_sec,delta_vanilla_sec,elapsed_sec,world\n");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ts = CSV_TIME_FMT.format(Instant.ofEpochMilli(rs.getLong("recorded_at")));
                    Long jets = getNullableLong(rs, "jets_seconds");
                    Long sess = getNullableLong(rs, "jets_session_seconds");
                    long vanilla = rs.getLong("vanilla_seconds");

                    w.write(String.join(",",
                            csv(ts),
                            csv(rs.getString("boot_id")),
                            csv(rs.getString("uuid")),
                            csv(rs.getString("player_name")),
                            csv(rs.getString("trigger_type")),
                            csv(jets != null ? formatDuration(jets) : ""),
                            csv(sess != null ? formatDuration(sess) : ""),
                            csv(formatDuration(vanilla)),
                            csv(jets),
                            csv(rs.getString("jets_raw")),
                            csv(sess),
                            csv(getNullableLong(rs, "jets_afk_seconds")),
                            csv(rs.getString("jets_is_afk_raw")),
                            csv(vanilla),
                            csv(getNullableLong(rs, "delta_jets")),
                            csv(getNullableLong(rs, "delta_session")),
                            csv(getNullableLong(rs, "delta_vanilla")),
                            csv(getNullableLong(rs, "elapsed_seconds")),
                            csv(rs.getString("world"))
                    ));
                    w.write('\n');
                    count++;
                }
            }
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Event Listeners & Main-Thread Capture
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!papiAvailable) return;
        UUID uuid = event.getPlayer().getUniqueId();
        readyPlayers.remove(uuid);

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return;

            readyPlayers.add(uuid);
            captureAndEnqueue(player, "JOIN", true, true);
        }, JOIN_DELAY_TICKS);
    }

    // LOWEST runs before JetsAntiAFKPro handles quit and evicts the player from memory
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!papiAvailable) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!readyPlayers.remove(uuid)) {
            return;
        }
        captureAndEnqueue(event.getPlayer(), "QUIT", false, false);
    }

    /**
     * Reads all Bukkit/PAPI state safely on the main thread, then hands off
     * rollback lookup, Jets-to-Jets comparison, and SQLite persistence to dbExecutor.
     */
    private void captureAndEnqueue(Player player, String trigger, boolean markInSession, boolean scheduleVerifyIfAnomaly) {
        long nowMillis = System.currentTimeMillis();
        String nowIso = Instant.ofEpochMilli(nowMillis).toString();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String world = player.getWorld().getName();

        String rawJets = PlaceholderAPI.setPlaceholders(player, PH_TOTAL_SECONDS);
        String rawAfkSeconds = PlaceholderAPI.setPlaceholders(player, PH_AFK_SECONDS);
        String rawIsAfk = PlaceholderAPI.setPlaceholders(player, PH_IS_AFK);

        Long jetsSeconds = parseLongOrNull(rawJets);
        Long sessionSeconds = readSessionSeconds(player);
        Long afkSeconds = parseLongOrNull(rawAfkSeconds);
        long vanillaSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;

        RawCapture capture = new RawCapture(
                nowMillis, nowIso, bootId, uuid, name, trigger,
                jetsSeconds, rawJets, sessionSeconds, afkSeconds, rawIsAfk,
                vanillaSeconds, world, markInSession, scheduleVerifyIfAnomaly
        );

        dbExecutor.submit(() -> processAndPersist(capture));
    }

    private Long readSessionSeconds(Player player) {
        Long d = parseLongOrNull(PlaceholderAPI.setPlaceholders(player, PH_SESSION_DAYS));
        Long h = parseLongOrNull(PlaceholderAPI.setPlaceholders(player, PH_SESSION_HOURS));
        Long m = parseLongOrNull(PlaceholderAPI.setPlaceholders(player, PH_SESSION_MINUTES));
        Long s = parseLongOrNull(PlaceholderAPI.setPlaceholders(player, PH_SESSION_SECONDS));
        if (d == null || h == null || m == null || s == null) {
            return null;
        }
        return (d * 86400L) + (h * 3600L) + (m * 60L) + s;
    }

    // -------------------------------------------------------------------------
    // Async Analysis, Rollback Fingerprinting & SQLite Persistence
    // -------------------------------------------------------------------------

    private void processAndPersist(RawCapture c) {
        if (!isConnectionOpen()) return;

        PlayerState prev = lastKnown.get(c.uuid);
        boolean crossedRestart = prev != null && prev.bootId != null && !prev.bootId.equals(c.bootId);

        Long deltaJets = (prev != null && prev.jetsSeconds != null && c.jetsSeconds != null)
                ? c.jetsSeconds - prev.jetsSeconds
                : null;
        Long deltaSession = (prev != null && prev.inSession && prev.sessionSeconds != null && c.sessionSeconds != null)
                ? c.sessionSeconds - prev.sessionSeconds
                : null;
        Long deltaVanilla = (prev != null)
                ? c.vanillaSeconds - prev.vanillaSeconds
                : null;
        Long elapsedSeconds = (prev != null)
                ? (c.recordedAt - prev.recordedAt) / 1000L
                : null;

        boolean notAfk = (c.afkSeconds == null || c.afkSeconds == 0L);
        boolean stalledThisTick = prev != null
                && prev.inSession
                && "PERIODIC".equals(c.trigger)
                && deltaJets != null && deltaJets == 0L
                && deltaVanilla != null && deltaVanilla >= STALL_MIN_VANILLA_DELTA
                && notAfk;

        int nextStallStreak = stalledThisTick ? (prev.consecutiveStalls + 1) : 0;

        Long currentPeak = maxNullable(prev != null ? prev.peakJetsSeconds : null, c.jetsSeconds);

        Anomaly anomaly = analyzeAnomaly(
                c, prev, crossedRestart, currentPeak,
                deltaJets, deltaSession, deltaVanilla,
                stalledThisTick, nextStallStreak
        );

        if (anomaly != null) {
            this.plugin.getLogger().warning(String.format(
                    "[PlaytimeAudit] %s for %s (%s) on %s%s: prev=%s -> curr=%s (peak=%s) | %s",
                    anomaly.type, c.playerName, c.uuid, c.trigger,
                    crossedRestart ? "+RESTART" : "",
                    anomaly.previousSeconds, anomaly.currentSeconds, currentPeak, anomaly.details
            ));

            if (c.scheduleVerifyIfAnomaly && !shuttingDown) {
                UUID targetUuid = c.uuid;
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    Player p2 = Bukkit.getPlayer(targetUuid);
                    if (p2 != null && p2.isOnline() && readyPlayers.contains(targetUuid)) {
                        captureAndEnqueue(p2, "JOIN_VERIFY", true, false);
                    }
                }, JOIN_VERIFY_DELAY_TICKS);
            }
        }

        Long updatedJets = (c.jetsSeconds != null) ? c.jetsSeconds : (prev != null ? prev.jetsSeconds : null);
        Long updatedSessionStart = "JOIN".equals(c.trigger)
                ? c.jetsSeconds
                : (prev != null ? prev.sessionStartJets : c.jetsSeconds);

        PlayerState nextState = new PlayerState(
                updatedJets,
                currentPeak,
                updatedSessionStart,
                c.sessionSeconds,
                c.vanillaSeconds,
                c.recordedAt,
                c.bootId,
                c.markInSession,
                nextStallStreak
        );
        lastKnown.put(c.uuid, nextState);

        Sample sample = new Sample(
                c.recordedAt, c.recordedIso, c.bootId, c.uuid.toString(), c.playerName, c.trigger,
                c.jetsSeconds, c.jetsRaw, c.sessionSeconds, c.afkSeconds, c.isAfkRaw,
                c.vanillaSeconds, deltaJets, deltaSession, deltaVanilla, elapsedSeconds, c.world
        );

        writeToDatabase(sample, nextState, anomaly, crossedRestart);
    }

    private Anomaly analyzeAnomaly(
            RawCapture c,
            PlayerState prev,
            boolean crossedRestart,
            Long peakJets,
            Long deltaJets,
            Long deltaSession,
            Long deltaVanilla,
            boolean stalledThisTick,
            int nextStallStreak
    ) {
        if (c.jetsSeconds == null) {
            return new Anomaly(
                    "PARSE_FAILURE",
                    prev != null ? prev.jetsSeconds : null,
                    null,
                    peakJets,
                    null,
                    null,
                    null,
                    "Placeholder returned non-numeric value: '" + c.jetsRaw + "'"
            );
        }

        if (prev == null || prev.jetsSeconds == null) {
            return null;
        }

        // 1. Playtime dropped (Jets current < Jets previous)
        if (c.jetsSeconds < prev.jetsSeconds) {
            long loss = prev.jetsSeconds - c.jetsSeconds;

            if (c.jetsSeconds == 0L) {
                return new Anomaly(
                        "RESET_TO_ZERO",
                        prev.jetsSeconds,
                        0L,
                        peakJets,
                        loss,
                        null,
                        null,
                        "Wiped to 0 (-" + formatDuration(loss) + ", " + (prev.inSession ? "mid-session" : "between sessions") + ")"
                );
            }

            // Check if JOIN reverted right back to the previous session's starting playtime
            if ("JOIN".equals(c.trigger)
                    && prev.sessionStartJets != null
                    && prev.jetsSeconds > prev.sessionStartJets + 30L
                    && Math.abs(c.jetsSeconds - prev.sessionStartJets) <= ROLLBACK_MATCH_TOLERANCE_SEC) {
                PastMatch match = findHistoricalMatch(c.uuid.toString(), c.jetsSeconds, c.recordedAt - 10_000L);
                Long matchedAt = match != null ? match.recordedAt : null;
                Long rbAge = matchedAt != null ? (c.recordedAt - matchedAt) / 1000L : null;
                String extra = matchedAt != null
                        ? " (matched " + CHAT_TIME_FMT.format(Instant.ofEpochMilli(matchedAt)) + ")"
                        : "";
                return new Anomaly(
                        "SESSION_NOT_SAVED",
                        prev.jetsSeconds,
                        c.jetsSeconds,
                        peakJets,
                        loss,
                        matchedAt,
                        rbAge,
                        "Reverted to previous session start; last session (-" + formatDuration(loss) + ") failed to save" + extra
                );
            }

            // Compare against historical Jets timestamps to distinguish ROLLBACK vs ACTIVE SUBTRACTION
            PastMatch match = findHistoricalMatch(c.uuid.toString(), c.jetsSeconds, c.recordedAt - 10_000L);
            if (match != null) {
                long rbAgeSec = Math.max(0L, (c.recordedAt - match.recordedAt) / 1000L);
                String pastTimeStr = CHAT_TIME_FMT.format(Instant.ofEpochMilli(match.recordedAt));
                return new Anomaly(
                        "ROLLBACK_TO_PAST",
                        prev.jetsSeconds,
                        c.jetsSeconds,
                        peakJets,
                        loss,
                        match.recordedAt,
                        rbAgeSec,
                        "Matched Jets value (" + match.jetsSeconds + "s) from " + pastTimeStr + " (" + formatDuration(rbAgeSec) + " ago, " + match.trigger + ")"
                );
            }

            // Did NOT match any historical Jets reading -> time was actively subtracted or corrupted
            boolean cleanHourMultiple = loss >= 3585L && (loss % 3600L <= 15L || loss % 3600L >= 3585L);
            String hourNote = cleanHourMultiple
                    ? " [EXACT ~" + Math.round(loss / 3600.0) + "h SUBTRACTION]"
                    : "";

            return new Anomaly(
                    "TIME_SUBTRACTED",
                    prev.jetsSeconds,
                    c.jetsSeconds,
                    peakJets,
                    loss,
                    null,
                    null,
                    "Dropped -" + formatDuration(loss) + " to unseen value (no past Jets timestamp match)" + hourNote
            );
        }

        // 2. Pure Jets-to-Jets internal check: Jets session timer reset mid-session
        if (prev.inSession
                && "PERIODIC".equals(c.trigger)
                && prev.sessionSeconds != null
                && c.sessionSeconds != null
                && c.sessionSeconds + 15L < prev.sessionSeconds) {
            return new Anomaly(
                    "SESSION_RESET_MIDGAME",
                    prev.jetsSeconds,
                    c.jetsSeconds,
                    peakJets,
                    0L,
                    null,
                    null,
                    "Jets session timer reset mid-session (" + formatDuration(prev.sessionSeconds) + " -> " + formatDuration(c.sessionSeconds) + ") without disconnect"
            );
        }

        // 3. Pure Jets-to-Jets internal check: Jets session timer gained 4+ mins, but Jets total gained 0
        if (prev.inSession
                && "PERIODIC".equals(c.trigger)
                && deltaSession != null && deltaSession >= STALL_MIN_VANILLA_DELTA
                && deltaJets != null && deltaJets == 0L) {
            return new Anomaly(
                    "JETS_INTERNAL_MISMATCH",
                    prev.jetsSeconds,
                    c.jetsSeconds,
                    peakJets,
                    0L,
                    null,
                    null,
                    "Jets session increased +" + formatDuration(deltaSession) + " but Jets total stayed at " + formatDuration(c.jetsSeconds)
            );
        }

        // 4. Vanilla vs Jets stall check (2 consecutive 5m windows = 10 active mins frozen)
        if (stalledThisTick && nextStallStreak >= STALL_CONSECUTIVE_REQUIRED) {
            return new Anomaly(
                    "STALLED",
                    prev.jetsSeconds,
                    c.jetsSeconds,
                    peakJets,
                    0L,
                    null,
                    null,
                    "Jets playtime frozen for " + nextStallStreak + " consecutive 5m checks (+" + deltaVanilla + "s vanilla, afkSec=" + c.afkSeconds + ")"
            );
        }

        return null;
    }

    private PastMatch findHistoricalMatch(String uuid, long targetJetsSeconds, long beforeMillis) {
        String sql = """
                SELECT recorded_at, trigger_type, jets_seconds
                FROM playtime_samples
                WHERE uuid = ?
                  AND jets_seconds IS NOT NULL
                  AND ABS(jets_seconds - ?) <= ?
                  AND recorded_at < ?
                ORDER BY recorded_at DESC
                LIMIT 1
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setLong(2, targetJetsSeconds);
            ps.setLong(3, ROLLBACK_MATCH_TOLERANCE_SEC);
            ps.setLong(4, beforeMillis);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PastMatch(
                            rs.getLong("recorded_at"),
                            rs.getString("trigger_type"),
                            rs.getLong("jets_seconds")
                    );
                }
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[PlaytimeAudit] Failed historical rollback lookup", e);
        }
        return null;
    }

    private boolean initDatabase() {
        try {
            File dataFolder = this.plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                return false;
            }

            this.dbFile = new File(dataFolder, "playtime_audit.db");
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA synchronous=NORMAL;");

                st.execute("""
                        CREATE TABLE IF NOT EXISTS playtime_samples (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            recorded_at INTEGER NOT NULL,
                            recorded_iso TEXT NOT NULL,
                            boot_id TEXT NOT NULL DEFAULT '',
                            uuid TEXT NOT NULL,
                            player_name TEXT NOT NULL,
                            trigger_type TEXT NOT NULL,
                            jets_seconds INTEGER,
                            jets_raw TEXT NOT NULL,
                            jets_session_seconds INTEGER,
                            jets_afk_seconds INTEGER,
                            jets_is_afk_raw TEXT,
                            vanilla_seconds INTEGER NOT NULL,
                            delta_jets INTEGER,
                            delta_session INTEGER,
                            delta_vanilla INTEGER,
                            elapsed_seconds INTEGER,
                            world TEXT NOT NULL
                        );
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS playtime_anomalies (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            recorded_at INTEGER NOT NULL,
                            recorded_iso TEXT NOT NULL,
                            boot_id TEXT NOT NULL DEFAULT '',
                            crossed_restart INTEGER NOT NULL DEFAULT 0,
                            uuid TEXT NOT NULL,
                            player_name TEXT NOT NULL,
                            trigger_type TEXT NOT NULL,
                            anomaly_type TEXT NOT NULL,
                            previous_seconds INTEGER,
                            current_seconds INTEGER,
                            peak_seconds INTEGER,
                            loss_seconds INTEGER,
                            matched_past_at INTEGER,
                            rollback_age_seconds INTEGER,
                            delta_vanilla INTEGER,
                            details TEXT
                        );
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS playtime_last_known (
                            uuid TEXT PRIMARY KEY,
                            player_name TEXT NOT NULL,
                            last_jets_seconds INTEGER,
                            peak_jets_seconds INTEGER,
                            session_start_jets INTEGER,
                            last_session_seconds INTEGER,
                            last_vanilla_seconds INTEGER NOT NULL,
                            last_recorded_at INTEGER NOT NULL,
                            last_boot_id TEXT NOT NULL DEFAULT ''
                        );
                        """);

                // Safe column migrations in case an earlier schema DB already exists
                ensureColumn(st, "playtime_samples", "boot_id", "TEXT NOT NULL DEFAULT ''");
                ensureColumn(st, "playtime_samples", "jets_session_seconds", "INTEGER");
                ensureColumn(st, "playtime_samples", "delta_session", "INTEGER");

                ensureColumn(st, "playtime_anomalies", "boot_id", "TEXT NOT NULL DEFAULT ''");
                ensureColumn(st, "playtime_anomalies", "crossed_restart", "INTEGER NOT NULL DEFAULT 0");
                ensureColumn(st, "playtime_anomalies", "peak_seconds", "INTEGER");
                ensureColumn(st, "playtime_anomalies", "matched_past_at", "INTEGER");
                ensureColumn(st, "playtime_anomalies", "rollback_age_seconds", "INTEGER");

                ensureColumn(st, "playtime_last_known", "peak_jets_seconds", "INTEGER");
                ensureColumn(st, "playtime_last_known", "session_start_jets", "INTEGER");
                ensureColumn(st, "playtime_last_known", "last_session_seconds", "INTEGER");
                ensureColumn(st, "playtime_last_known", "last_boot_id", "TEXT NOT NULL DEFAULT ''");

                st.execute("CREATE INDEX IF NOT EXISTS idx_samples_uuid_time ON playtime_samples(uuid, recorded_at);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_samples_uuid_jets ON playtime_samples(uuid, jets_seconds);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_samples_name_time ON playtime_samples(player_name COLLATE NOCASE, recorded_at);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_anomalies_time ON playtime_anomalies(recorded_at);");
            }
            return true;
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "[PlaytimeAudit] SQLite init error", e);
            return false;
        }
    }

    private static void ensureColumn(Statement st, String table, String column, String definition) {
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + ";");
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    private void loadLastKnownStates() {
        String sql = """
                SELECT uuid, last_jets_seconds, peak_jets_seconds, session_start_jets,
                       last_session_seconds, last_vanilla_seconds, last_recorded_at, last_boot_id
                FROM playtime_last_known
                """;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                Long jets = getNullableLong(rs, "last_jets_seconds");
                Long peak = getNullableLong(rs, "peak_jets_seconds");
                if (peak == null) peak = jets;
                Long sessStart = getNullableLong(rs, "session_start_jets");
                Long lastSess = getNullableLong(rs, "last_session_seconds");
                long vanilla = rs.getLong("last_vanilla_seconds");
                long recordedAt = rs.getLong("last_recorded_at");
                String lastBoot = rs.getString("last_boot_id");

                lastKnown.put(uuid, new PlayerState(jets, peak, sessStart, lastSess, vanilla, recordedAt, lastBoot, false, 0));
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[PlaytimeAudit] Failed loading last-known states", e);
        }
    }

    private void writeToDatabase(Sample s, PlayerState state, Anomaly anomaly, boolean crossedRestart) {
        if (!isConnectionOpen()) return;

        String insertSample = """
                INSERT INTO playtime_samples (
                    recorded_at, recorded_iso, boot_id, uuid, player_name, trigger_type,
                    jets_seconds, jets_raw, jets_session_seconds, jets_afk_seconds, jets_is_afk_raw,
                    vanilla_seconds, delta_jets, delta_session, delta_vanilla, elapsed_seconds, world
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String upsertLastKnown = """
                INSERT INTO playtime_last_known (
                    uuid, player_name, last_jets_seconds, peak_jets_seconds, session_start_jets,
                    last_session_seconds, last_vanilla_seconds, last_recorded_at, last_boot_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    last_jets_seconds = excluded.last_jets_seconds,
                    peak_jets_seconds = excluded.peak_jets_seconds,
                    session_start_jets = excluded.session_start_jets,
                    last_session_seconds = excluded.last_session_seconds,
                    last_vanilla_seconds = excluded.last_vanilla_seconds,
                    last_recorded_at = excluded.last_recorded_at,
                    last_boot_id = excluded.last_boot_id
                """;

        String insertAnomaly = """
                INSERT INTO playtime_anomalies (
                    recorded_at, recorded_iso, boot_id, crossed_restart, uuid, player_name, trigger_type,
                    anomaly_type, previous_seconds, current_seconds, peak_seconds, loss_seconds,
                    matched_past_at, rollback_age_seconds, delta_vanilla, details
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(insertSample)) {
                ps.setLong(1, s.recordedAt);
                ps.setString(2, s.recordedIso);
                ps.setString(3, s.bootId);
                ps.setString(4, s.uuid);
                ps.setString(5, s.playerName);
                ps.setString(6, s.trigger);
                setNullableLong(ps, 7, s.jetsSeconds);
                ps.setString(8, s.jetsRaw);
                setNullableLong(ps, 9, s.sessionSeconds);
                setNullableLong(ps, 10, s.afkSeconds);
                ps.setString(11, s.isAfkRaw);
                ps.setLong(12, s.vanillaSeconds);
                setNullableLong(ps, 13, s.deltaJets);
                setNullableLong(ps, 14, s.deltaSession);
                setNullableLong(ps, 15, s.deltaVanilla);
                setNullableLong(ps, 16, s.elapsedSeconds);
                ps.setString(17, s.world);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(upsertLastKnown)) {
                ps.setString(1, s.uuid);
                ps.setString(2, s.playerName);
                setNullableLong(ps, 3, state.jetsSeconds);
                setNullableLong(ps, 4, state.peakJetsSeconds);
                setNullableLong(ps, 5, state.sessionStartJets);
                setNullableLong(ps, 6, state.sessionSeconds);
                ps.setLong(7, state.vanillaSeconds);
                ps.setLong(8, state.recordedAt);
                ps.setString(9, state.bootId != null ? state.bootId : "");
                ps.executeUpdate();
            }

            if (anomaly != null) {
                try (PreparedStatement ps = connection.prepareStatement(insertAnomaly)) {
                    ps.setLong(1, s.recordedAt);
                    ps.setString(2, s.recordedIso);
                    ps.setString(3, s.bootId);
                    ps.setInt(4, crossedRestart ? 1 : 0);
                    ps.setString(5, s.uuid);
                    ps.setString(6, s.playerName);
                    ps.setString(7, s.trigger);
                    ps.setString(8, anomaly.type);
                    setNullableLong(ps, 9, anomaly.previousSeconds);
                    setNullableLong(ps, 10, anomaly.currentSeconds);
                    setNullableLong(ps, 11, anomaly.peakSeconds);
                    setNullableLong(ps, 12, anomaly.lossSeconds);
                    setNullableLong(ps, 13, anomaly.matchedPastAt);
                    setNullableLong(ps, 14, anomaly.rollbackAgeSeconds);
                    setNullableLong(ps, 15, s.deltaVanilla);
                    ps.setString(16, anomaly.details);
                    ps.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            this.plugin.getLogger().log(Level.WARNING, "[PlaytimeAudit] Failed to write sample for " + s.playerName, e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isConnectionOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private void sendSync(CommandSender sender, List<Component> messages) {
        if (shuttingDown) return;
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (Component msg : messages) {
                sender.sendMessage(msg);
            }
        });
    }

    private static NamedTextColor colorForAnomaly(String type) {
        return switch (type) {
            case "RESET_TO_ZERO", "TIME_SUBTRACTED" -> NamedTextColor.RED;
            case "ROLLBACK_TO_PAST", "SESSION_NOT_SAVED" -> NamedTextColor.GOLD;
            case "JETS_INTERNAL_MISMATCH", "SESSION_RESET_MIDGAME", "STALLED" -> NamedTextColor.YELLOW;
            default -> NamedTextColor.LIGHT_PURPLE;
        };
    }

    private static Long maxNullable(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    private static List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    private static List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(opt);
            }
        }
        return matches;
    }

    private static int parseLimit(String raw, int fallback, int max) {
        try {
            int val = Integer.parseInt(raw);
            return Math.max(1, Math.min(val, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String formatDuration(long totalSeconds) {
        boolean neg = totalSeconds < 0;
        long sec = Math.abs(totalSeconds);
        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long mins = (sec % 3600) / 60;
        long secs = sec % 60;

        StringBuilder sb = new StringBuilder();
        if (neg) sb.append('-');
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (mins > 0 || hours > 0 || days > 0) sb.append(mins).append("m ");
        sb.append(secs).append('s');
        return sb.toString();
    }

    private static String formatSignedDelta(Long seconds) {
        if (seconds == null) return "n/a";
        if (seconds > 0) return "+" + formatDuration(seconds);
        if (seconds < 0) return formatDuration(seconds);
        return "+0s";
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String str = String.valueOf(value);
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private static Long getNullableLong(ResultSet rs, String col) throws SQLException {
        long val = rs.getLong(col);
        return rs.wasNull() ? null : val;
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, value);
        }
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replace(",", "").trim();
        if (cleaned.isEmpty() || cleaned.startsWith("%")) return null;
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            try {
                return (long) Double.parseDouble(cleaned);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private record RawCapture(
            long recordedAt,
            String recordedIso,
            String bootId,
            UUID uuid,
            String playerName,
            String trigger,
            Long jetsSeconds,
            String jetsRaw,
            Long sessionSeconds,
            Long afkSeconds,
            String isAfkRaw,
            long vanillaSeconds,
            String world,
            boolean markInSession,
            boolean scheduleVerifyIfAnomaly
    ) {}

    private record PlayerState(
            Long jetsSeconds,
            Long peakJetsSeconds,
            Long sessionStartJets,
            Long sessionSeconds,
            long vanillaSeconds,
            long recordedAt,
            String bootId,
            boolean inSession,
            int consecutiveStalls
    ) {}

    private record PastMatch(long recordedAt, String trigger, long jetsSeconds) {}

    private record Sample(
            long recordedAt,
            String recordedIso,
            String bootId,
            String uuid,
            String playerName,
            String trigger,
            Long jetsSeconds,
            String jetsRaw,
            Long sessionSeconds,
            Long afkSeconds,
            String isAfkRaw,
            long vanillaSeconds,
            Long deltaJets,
            Long deltaSession,
            Long deltaVanilla,
            Long elapsedSeconds,
            String world
    ) {}

    private record Anomaly(
            String type,
            Long previousSeconds,
            Long currentSeconds,
            Long peakSeconds,
            Long lossSeconds,
            Long matchedPastAt,
            Long rollbackAgeSeconds,
            String details
    ) {}
}