package util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import express.Express;
import express.http.request.Request;
import express.http.response.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Listener {
    public enum ListenerType {
        GET,
        POST,
        ALL,
        DELETE,
        PUT,
        PATCH
    }
    private int port;
    private final HashMap<String, Map.Entry<ListenerType,BiConsumer<JsonObject, Response>>> keys = new HashMap<>();
    public Listener(int port) {
        this.port = port;
        Statics.app.all("*",(req,res) -> {
            Statics.log(req.getMethod() + " " + req.getURI().toString());
        });
    }
    public void addListener(String key, ListenerType type, BiConsumer<JsonObject, Response> onListen) {
        keys.put(key,new AbstractMap.SimpleEntry<>(type,onListen));
    }
    public void listen() {
        Express e = Statics.app;
        keys.forEach((k,v) -> {
            switch (keys.get(k).getKey()) {
                case GET:
                    e.get(k,(req,res) -> manage(k,req,res));
                    break;
                case POST:
                    e.post(k,(req,res) -> manage(k,req,res));
                    break;
                case ALL:
                    e.all(k,(req,res) -> manage(k,req,res));
                    break;
                case DELETE:
                    e.delete(k,(req,res) -> manage(k,req,res));
                    break;
                case PUT:
                    e.put(k,(req,res) -> manage(k,req,res));
                case PATCH:
                    e.patch(k,(req,res) -> manage(k,req,res));
            }
        });
        e.listen(port);
    }
    public void manage(String key, Request request, Response response) {
        JsonObject jo = new JsonObject();
        jo.addProperty("user_agent",request.getUserAgent());
        jo.addProperty("auth",String.join(", ",request.getHeader("Authorization")));
        jo.add("body",new Gson().fromJson(new BufferedReader(new InputStreamReader(request.getBody(), StandardCharsets.US_ASCII)).lines().collect(Collectors.joining("\n")), JsonElement.class));
        request.getFormQuerys().forEach(jo::addProperty);
        request.getQuerys().forEach(jo::addProperty);
        request.getParams().forEach(jo::addProperty);
        request.getCookies().forEach((str,cookie) -> jo.addProperty(str,cookie.getValue()));
        keys.get(key).getValue().accept(jo,response);
    }
}
