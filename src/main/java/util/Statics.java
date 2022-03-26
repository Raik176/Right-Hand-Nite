package util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import express.Express;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class Statics {
    public static Express app = new Express();
    public static JsonObject config;
    public static JsonObject items;
    public static JsonArray bundles;
    public static void log(Object msg) {
        System.out.println("LOG  -  -  [" + consoleDate() + "] " + msg);
    }
    public static void info(Object msg) {
        System.out.println((char)27 + "[36mINFO  -  -  [" + consoleDate() + "] " + msg + (char)27 + "[0m");
    }
    public static void warn(Object msg) {
        System.out.println((char)27 + "[33mWARNING  -  -  [" + consoleDate() + "] " + msg + (char)27 + "[0m");
    }
    public static void error(Object msg) {
        System.out.println((char)27 + "[31mERROR  -  -  [" + consoleDate() + "] " + msg + (char)27 + "[0m");
    }
    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS'Z'");
        return sdf.format(new Date());
    }
    public static JsonObject readJson(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            StringBuilder out = new StringBuilder();
            String line = "";
            while ((line = br.readLine()) != null) out.append(line);
            return new Gson().fromJson(out.toString(),JsonObject.class);
        } catch (IOException e) {
            warn("Couldn't load json " + path + ", please try to restart the server. If the problem persists contact the owners of the backend!");
            return null;
        }
    }

    public static JsonElement get(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            return new Gson().fromJson(new BufferedReader(new InputStreamReader(con.getInputStream())), JsonElement.class);
        } catch (IOException e) {
            Statics.warn("Get request to " + url.toString() + " failed, please try to restart the server. If the problem persists conact the owners of the backend!");
            return null;
        }
    }
    public static JsonElement post(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.connect();
            return new Gson().fromJson(new BufferedReader(new InputStreamReader(con.getInputStream())), JsonElement.class);
        } catch (IOException e) {
            Statics.warn("Post request to " + url.toString() + " failed, please try to restart the server. If the problem persists conact the owners of the backend!");
            return null;
        }
    }

    private static String consoleDate() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT+1"));
        return (format.format(c.getTime()));
    }
}
