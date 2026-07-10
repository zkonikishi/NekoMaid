package cn.apisium.nekomaid.builtin;

import cn.apisium.nekomaid.NekoMaid;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class LocalWebFrontend {
    private final NekoMaid main;
    private final AiAssistant ai;
    private final NekoMaid.HttpRouteHandler handler;

    LocalWebFrontend(NekoMaid main, AiAssistant ai) {
        this.main = main;
        this.ai = ai;
        this.handler = this::handle;
        main.addHttpRouteHandler(handler);
    }

    void disable() {
        main.removeHttpRouteHandler(handler);
    }

    private boolean handle(ChannelHandlerContext context, FullHttpRequest request) {
        QueryStringDecoder query = new QueryStringDecoder(request.uri());
        String path = query.path();
        if ("/api/status".equals(path) && request.method().equals(HttpMethod.GET)) {
            if (!isAuthorized(query)) {
                writeJson(context, HttpResponseStatus.UNAUTHORIZED, new JSONObject()
                        .put("ok", false)
                        .put("error", "BAD_TOKEN"));
                return true;
            }
            writeJson(context, HttpResponseStatus.OK, status());
            return true;
        }
        if ("/api/ai/ask".equals(path) && request.method().equals(HttpMethod.POST)) {
            if (!isAuthorized(query)) {
                writeJson(context, HttpResponseStatus.UNAUTHORIZED, new JSONObject()
                        .put("ok", false)
                        .put("error", "BAD_TOKEN"));
                return true;
            }
            String body = request.content().toString(StandardCharsets.UTF_8);
            String question = new JSONObject(body.isEmpty() ? "{}" : body).optString("question", "").trim();
            if (question.isEmpty()) {
                writeJson(context, HttpResponseStatus.BAD_REQUEST, new JSONObject()
                        .put("ok", false)
                        .put("error", "EMPTY_QUESTION"));
                return true;
            }
            Bukkit.getScheduler().runTaskAsynchronously(main,
                    () -> writeJson(context, HttpResponseStatus.OK, ai.ask(question)));
            return true;
        }
        if (query.parameters().containsKey("EIO") || query.parameters().containsKey("transport") ||
                HttpHeaderValues.WEBSOCKET.toString().equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE))) {
            return false;
        }
        if (request.method().equals(HttpMethod.GET) && serveFrontend(context, path)) return true;
        return false;
    }

    private boolean serveFrontend(ChannelHandlerContext context, String path) {
        String resource = path.equals("/") ? "index.html" : path.substring(1);
        if (resource.contains("..") || resource.startsWith("api/")) return false;
        byte[] content = readResource("webui/" + resource);
        if (content == null && !resource.contains(".")) {
            resource = "index.html";
            content = readResource("webui/index.html");
        }
        if (content == null) return false;
        write(context, HttpResponseStatus.OK, contentType(resource), content);
        return true;
    }

    private static byte[] readResource(String path) {
        try (InputStream input = LocalWebFrontend.class.getClassLoader().getResourceAsStream(path)) {
            return input == null ? null : input.readAllBytes();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private JSONObject status() {
        return new JSONObject()
                .put("ok", true)
                .put("serverVersion", Bukkit.getVersion())
                .put("javaVersion", System.getProperty("java.version"))
                .put("onlinePlayers", Bukkit.getOnlinePlayers().size())
                .put("maxPlayers", Bukkit.getMaxPlayers())
                .put("pluginVersion", main.getDescription().getVersion())
                .put("clients", main.getClientsCount())
                .put("ai", ai.getStatus());
    }

    private boolean isAuthorized(QueryStringDecoder query) {
        String expected = main.getConfig().getString("token", "");
        String token = first(query.parameters(), "token");
        return expected != null && !expected.isEmpty() && expected.equals(token);
    }

    private static String first(Map<String, List<String>> parameters, String name) {
        List<String> values = parameters.get(name);
        return values == null || values.isEmpty() ? "" : values.get(0);
    }

    private static void writeJson(ChannelHandlerContext context, HttpResponseStatus status, JSONObject body) {
        write(context, status, "application/json; charset=utf-8", body.toString());
    }

    private static void write(ChannelHandlerContext context, HttpResponseStatus status, String contentType, String body) {
        write(context, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(ChannelHandlerContext context, HttpResponseStatus status, String contentType, byte[] bytes) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes));
        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, contentType)
                .set(HttpHeaderNames.CONTENT_LENGTH, bytes.length)
                .set(HttpHeaderNames.CACHE_CONTROL, "no-store")
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
