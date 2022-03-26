package util;

import express.utils.Status;

import java.text.MessageFormat;
import java.util.UUID;

public class OAuthManager extends Manager {

    public OAuthManager(Listener l) {
        super(l);
    }

    @Override
    public void listen(Listener l) {
        l.addListener("/account/api/oauth/token", Listener.ListenerType.POST,(req,res) -> {
            String displayName = "";
            String id = "";
            System.out.println(req.get("grant_type").getAsString());
            switch (req.get("grant_type").getAsString()) {
                case "client_credentials":
                    System.out.println(req);
                    displayName = "";
                    id = "";
                    break;
                case "password":
                    System.out.println(req);
                    if (!req.has("username")) {
                        res.setStatus(Status._404);
                        res.send("");
                        return;
                    }

                    displayName = req.get("username").getAsString();
                    id = displayName.replaceAll(" ","_");
                    break;
                default:
                    Statics.log(req.get("grant_type").getAsString());
                    break;
            }
            res.send("{\"access_token\":\"" + UUID.randomUUID().toString() +"\",\"expires_in\":28800,\"expires_at\":\"9999-12-31T23:59:59.999Z\",\"token_type\":\"bearer\",\"account_id\":\"" + id +"\",\"client_id\":\"ec684b8c687f479fadea3cb2ad83f5c6\",\"internal_client\":true,\"client_service\":\"fortnite\",\"displayName\":\"" + displayName + "\",\"app\":\"fortnite\",\"in_app_id\":\"" + id + "\",\"device_id\":\"5dcab5dbe86a7344b061ba57cdb33c4f\"}");
        });
        l.addListener("/account/api/oauth/sessions/kill", Listener.ListenerType.DELETE,(req,res) -> {
            System.out.println(req);
            res.sendStatus(Status._204);
        });
        l.addListener("/account/api/public/account", Listener.ListenerType.GET, (req,res) -> res.send("{\"id\":\"Raik176\",\"displayName\":\"Raik176\",\"externalAuths\":{}}"));
        l.addListener("/account/api/public/account/:accountId", Listener.ListenerType.GET, (req,res) -> res.send("{\"id\":\"" + req.get("accountId").getAsString() + "\",\"displayName\":\"" + req.get("accountId").getAsString() +"\",\"externalAuths\":{}}"));
        l.addListener("/account/api/public/account/:accountId/externalAuths", Listener.ListenerType.GET, (req,res) -> res.send("[]"));
        l.addListener("/account/api/oauth/verify",Listener.ListenerType.GET,(req,res) -> res.send(Statics.readJson("response_templates/oauth_verify.json").toString().replace("{accessToken}",req.get("auth").getAsString().replace("bearer ","")).replace("{displayName}","")));
        l.addListener("/account/api/oauth/exchange", Listener.ListenerType.GET,(req,res) -> res.send("{\"expiresInSeconds\":9999999,\"code\":\"bb2dc02b58e54849897cbb0f65d35e5c\",\"creatingClientId\":\"ec684b8c687f479fadea3cb2ad83f5c6\"}"));
    }
}
