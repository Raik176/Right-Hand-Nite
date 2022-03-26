import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import express.utils.MediaType;
import express.utils.Status;
import util.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;


public class Main {
    public static void main(String[] args) {
        new Main().run();
    }
    public void run() {
        //Config update
        new Thread(() -> {
            while (true) {
                try {
                    Statics.config = Statics.readJson("jsons/config.json");
                    Thread.sleep(300000);
                } catch (InterruptedException ignored) {

                }
            }
        }).start();

        Listener l = new Listener(5595);

        l.addListener("/fortnite/api/cloudstorage/user/:accountId/:fileName", Listener.ListenerType.PUT,(req,res) -> res.sendStatus(Status._204));
        l.addListener("/fortnite/api/cloudstorage/user/:accountId",Listener.ListenerType.GET,(req,res) -> res.sendStatus(Status._204));

        l.addListener("/fortnite/api/cloudstorage/system/config", Listener.ListenerType.GET,(req,res) -> res.send("{\"lastUpdated\":\"2021-02-17T04:21:28.383Z\",\"disableV2\":false,\"isAuthenticated\":true,\"enumerateFilesPath\":\"/api/cloudstorage/system\",\"transports\":{\"McpProxyTransport\":{\"name\":\"McpProxyTransport\",\"type\":\"ProxyStreamingFile\",\"appName\":\"fortnite\",\"isEnabled\":true,\"isRequired\":true,\"isPrimary\":true,\"timeoutSeconds\":30,\"priority\":10},\"McpSignatoryTransport\":{\"name\":\"McpSignatoryTransport\",\"type\":\"ProxySignatory\",\"appName\":\"fortnite\",\"isEnabled\":false,\"isRequired\":false,\"isPrimary\":false,\"timeoutSeconds\":30,\"priority\":20},\"DssDirectTransport\":{\"name\":\"DssDirectTransport\",\"type\":\"DirectDss\",\"appName\":\"fortnite\",\"isEnabled\":true,\"isRequired\":false,\"isPrimary\":false,\"timeoutSeconds\":30,\"priority\":30}}}"));
        l.addListener("/fortnite/api/cloudstorage/system", Listener.ListenerType.GET,(req,res) -> {
            JsonArray output = new JsonArray();
            for (File f :  new File("hotfixes").listFiles()) {
                JsonObject file = new JsonObject();
                file.addProperty("uniqueFilename",f.getName());
                file.addProperty("filename",f.getName());
                try {
                    String con = String.join("\n",Files.readAllLines(f.toPath()));
                    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
                    byte[] digest = sha1.digest(con.getBytes(StandardCharsets.UTF_8));
                    StringBuilder s1 = new StringBuilder();
                    for (byte value : digest) {
                        s1.append(Integer.toHexString((value & 0xFF) | 0x100).substring(1, 3));
                    }
                    file.addProperty("hash",s1.toString());

                    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    byte[]hashInBytes = sha256.digest(con.getBytes(StandardCharsets.UTF_8));

                    StringBuilder s256 = new StringBuilder();
                    for (byte b : hashInBytes) {
                        s256.append(String.format("%02x", b));
                    }
                    file.addProperty("hash256",s256.toString());
                    file.addProperty("length",con.length());
                    file.addProperty("uploaded", String.valueOf(Files.readAttributes(f.toPath(), BasicFileAttributes.class).creationTime()));
                } catch (Exception e) {

                }
                file.addProperty("contentType","text/plain");
                file.addProperty("storageType","S3");
                file.addProperty("doNotCache",false);
                output.add(file);
            }
            res.setContentType(MediaType._json);
            res.send(output.toString());
        });
        l.addListener("/fortnite/api/cloudstorage/system/:file", Listener.ListenerType.GET,(req,res) -> {
            try {
                File f = new File("hotfixes/"+req.get("file").getAsString());
                if (f.exists()) {
                    res.send(f.toPath());
                } else {
                    res.sendStatus(Status._404);
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.sendStatus(Status._404);
            }
        });

        l.addListener("/datarouter/api/v1/public/data",Listener.ListenerType.POST,(req,res) -> {
            Statics.info(req);
            res.sendStatus(Status._204);
        });
        l.addListener("/v1/avatar/fortnite/ids", Listener.ListenerType.GET,(req,res) -> {
            try {
                res.setContentType(MediaType._json);
                String id = req.get("accountIds").getAsString();
                JsonArray output = new JsonArray();
                JsonObject out = new JsonObject();
                out.addProperty("accountId",id);
                out.addProperty("namespace","fortnite");
                out.addProperty("avatarId",Statics.readJson("accounts/"+id+"/profiles/profile_athena.json").get("items").getAsJsonObject().get("sandbox_loadout").getAsJsonObject().get("attributes").getAsJsonObject().get("locker_slots_data").getAsJsonObject().get("slots").getAsJsonObject().get("Character").getAsJsonObject().get("items").getAsJsonArray().get(0).getAsString());
                output.add(out);
                res.send(output.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        l.addListener("/fortnite/api/game/v2/creative/history/:accountId", Listener.ListenerType.GET,(req,res) -> res.send("{\"results\":[],\"hasMore\":false}"));
        l.addListener("/fortnite/api/game/v2/creative/favorites/:accountId", Listener.ListenerType.GET,(req,res) -> res.send("{\"results\":[],\"hasMore\":false}"));
        l.addListener("/statsproxy/api/statsv2/account/:accountId", Listener.ListenerType.GET,(req,res) -> res.send(Statics.readJson("response_templates/statsproxy.json").toString()));
        l.addListener("/catalog/api/shared/bulk/offers", Listener.ListenerType.GET,(req,res) -> res.send(Statics.readJson("response_templates/bulkoffers.json").toString()));
        l.addListener("/api/v2/interactions/latest/Fortnite/:accountId", Listener.ListenerType.GET,(req,res) -> res.send("{\"latestInteractions\":[]}"));
        l.addListener("/api/v2/interactions/aggregated/Fortnite/:accountId", Listener.ListenerType.GET,(req,res) -> res.send("{\"aggregatedInteractions\":[]}"));
        l.addListener("/content/api/pages/fortnite-game/media-events", Listener.ListenerType.GET,(req,res) -> res.send("{\"jcr:isCheckedOut\":true,\"mediaEvents\":{\"_type\":\"Fortnite-MediaEventList\"},\"_title\":\"media-events\",\"_noIndex\":false,\"jcr:baseVersion\":\"a7ca237317f1e7b3accf62-6322-4d35-8846-7a66ed54aa96\",\"_activeDate\":\"2022-01-12T01:43:01.626Z\",\"lastModified\":\"2022-01-24T17:15:52.789Z\",\"_locale\":\"de\",\"_suggestedPrefetch\":[]}"));
        l.addListener("/api/v1/events/Fortnite/download/:accountId", Listener.ListenerType.GET,(req,res) -> {
            res.sendStatus(Status._204);
        });
        new OAuthManager(l);
        new APIManager(l);
        new MCPManager(l);
        new PartyManager(l);
        new XMPPManager(l);

        l.listen();
        Statics.info("Started Server!");
    }
}
