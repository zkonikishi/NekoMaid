package cn.apisium.nekomaid.builtin;

import cn.apisium.nekomaid.NekoMaid;
import cn.apisium.nekomaid.utils.Utils;
import io.socket.socketio.server.SocketIoSocket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

final class AiAssistant {
    private final NekoMaid main;
    private final Terminal terminal;

    public AiAssistant(NekoMaid main, Terminal terminal) {
        this.main = main;
        this.terminal = terminal;
        main.GLOBAL_DATA.put("hasAiAssistant", true);
        main.onConnected(main, client -> client.onWithAck("ai:status", (Function<Object[], JSONObject>) args -> getStatus())
                .on("ai:ask", args -> {
                    SocketIoSocket.ReceivedByLocalAcknowledgementCallback ack =
                            (SocketIoSocket.ReceivedByLocalAcknowledgementCallback) args[args.length - 1];
                    String question = args.length == 0 || args[0] == null ? "" : String.valueOf(args[0]);
                    if (question.trim().isEmpty()) {
                        ack.sendAcknowledgement(Utils.serialize((Object) error("EMPTY_QUESTION")));
                        return;
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(main,
                            () -> ack.sendAcknowledgement(Utils.serialize((Object) ask(question))));
                }));
        main.registerCommand(main, "ai", new cn.apisium.nekomaid.NekoMaidCommand() {
            @Override
            public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                     @NotNull String label, @NotNull String[] args) {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.YELLOW + "[NekoMaid] Usage: /nekomaid ai <question>");
                    return true;
                }
                String question = String.join(" ", args);
                sender.sendMessage(ChatColor.YELLOW + "[NekoMaid] AI is analyzing recent logs...");
                Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                    JSONObject result = ask(question);
                    if (!result.optBoolean("ok")) {
                        sender.sendMessage(ChatColor.RED + "[NekoMaid] AI failed: " + result.optString("error"));
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "[NekoMaid] AI diagnosis:");
                    for (String line : result.optString("answer").split("\\R")) {
                        if (!line.trim().isEmpty()) sender.sendMessage(ChatColor.GRAY + line);
                    }
                });
                return true;
            }

            @Override
            public String[] getUsages() {
                return new String[] { "<question>" };
            }

            @Override
            public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String alias, @NotNull String[] args) {
                return Collections.emptyList();
            }
        });
    }

    JSONObject getStatus() {
        return new JSONObject()
                .put("enabled", main.getConfig().getBoolean("ai.enabled", false))
                .put("configured", isConfigured())
                .put("model", main.getConfig().getString("ai.model", ""))
                .put("baseUrl", main.getConfig().getString("ai.base-url", ""));
    }

    boolean isConfigured() {
        return main.getConfig().getBoolean("ai.enabled", false)
                && !main.getConfig().getString("ai.base-url", "").isEmpty()
                && !main.getConfig().getString("ai.model", "").isEmpty()
                && !main.getConfig().getString("ai.api-key", "").isEmpty();
    }

    JSONObject ask(String question) {
        if (!isConfigured()) return error("AI_NOT_CONFIGURED");
        try {
            JSONObject payload = new JSONObject()
                    .put("model", main.getConfig().getString("ai.model", "gpt-4.1-mini"))
                    .put("messages", new JSONArray()
                            .put(message("system", main.getConfig().getString("ai.system-prompt", "")))
                            .put(message("user", buildPrompt(question))));
            JSONObject response = postChatCompletions(payload);
            JSONArray choices = response.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return error("EMPTY_RESPONSE");
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            String content = message == null ? choices.getJSONObject(0).optString("text", "") : message.optString("content", "");
            if (content.trim().isEmpty()) return error("EMPTY_RESPONSE");
            return new JSONObject().put("ok", true).put("answer", content);
        } catch (Throwable e) {
            if (main.isDebug()) e.printStackTrace();
            return error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String buildPrompt(String question) {
        String plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(Plugin::getDescription)
                .map(it -> it.getName() + " " + it.getVersion())
                .collect(Collectors.joining(", "));
        int maxLines = main.getConfig().getInt("ai.max-log-lines", 120);
        return "Server version: " + Bukkit.getVersion() + "\n"
                + "Java version: " + System.getProperty("java.version") + "\n"
                + "Online players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers() + "\n"
                + "Plugins: " + plugins + "\n\n"
                + "Recent server logs:\n```log\n" + terminal.getRecentLogText(maxLines) + "\n```\n\n"
                + "Admin question:\n" + question + "\n\n"
                + "Return a practical diagnosis with likely cause, evidence from logs, repair steps, and risk notes.";
    }

    private JSONObject postChatCompletions(JSONObject payload) throws Exception {
        String baseUrl = main.getConfig().getString("ai.base-url", "").replaceAll("/+$", "");
        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(main.getConfig().getInt("ai.timeout-seconds", 60) * 1000);
        connection.setReadTimeout(main.getConfig().getInt("ai.timeout-seconds", 60) * 1000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + main.getConfig().getString("ai.api-key", ""));
        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }
        int status = connection.getResponseCode();
        InputStream in = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String text;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            text = reader.lines().collect(Collectors.joining("\n"));
        }
        if (status < 200 || status >= 300) throw new IllegalStateException("AI request failed: HTTP " + status + " " + text);
        return new JSONObject(text);
    }

    private static JSONObject message(String role, String content) {
        return new JSONObject().put("role", role).put("content", content == null ? "" : content);
    }

    private static JSONObject error(String message) {
        return new JSONObject().put("ok", false).put("error", message);
    }
}
