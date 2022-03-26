package util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import express.utils.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PartyManager extends Manager {
    private final List<Party> parties;
    public PartyManager(Listener l) {
        super(l);
        parties = new ArrayList<>();
    }

    @Override
    public void listen(Listener l) {
        l.addListener("/party/api/v1/:game/user/:accountId", Listener.ListenerType.GET, (req,res) -> {
            try {
                AtomicReference<Party> atomic = new AtomicReference<>();
                parties.stream().filter(party -> party.getMembers().stream().anyMatch(pm -> pm.id.equalsIgnoreCase(req.get("accountId").getAsString()))).findFirst().ifPresentOrElse(atomic::set, () -> {
                    Party p = new Party(UUID.randomUUID().toString());
                    p.addMember(req.get("accountId").getAsString());
                    p.setPartyLeader(req.get("accountId").getAsString());
                    atomic.set(p);
                });
                Party p = atomic.get();
                JsonObject response = new Gson().fromJson("{\"current\":[{\"config\":{\"type\":\"DEFAULT\",\"joinability\":\"INVITE_AND_FORMER\",\"discoverability\":\"INVITED_ONLY\",\"sub_type\":\"default\",\"max_size\":16,\"invite_ttl\":14400,\"join_confirmation\":true},\"members\":[],\"applicants\":[],\"meta\":{},\"invites\":[],\"revision\":0}],\"pending\":[],\"invites\":[],\"pings\":[]}".replaceAll("\\{party}", p.id).replaceAll("\\{date}", Statics.getDate()), JsonObject.class);
                JsonArray members = new JsonArray();
                for (PartyMember pm : p.getMembers()) {
                    JsonObject member = new JsonObject();
                    member.addProperty("account_id", pm.id);
                    member.add("meta", new Gson().fromJson("{\"urn:epic:member:dn_s\":\"" + pm.id + "\"}", JsonObject.class));
                    JsonArray connections = new JsonArray();
                    JsonObject connection = new JsonObject();
                    connection.addProperty("id", "");
                    connection.addProperty("connected_at", Statics.getDate());
                    connection.addProperty("updated_at", Statics.getDate());
                    connection.addProperty("yield_leadership", false);
                    connection.add("meta", new Gson().fromJson("{\"urn:epic:conn:platform_s\":\"WIN\",\"urn:epic:conn:type_s\":\"game\"}", JsonObject.class));
                    connections.add(connection);
                    member.add("connections", connections);
                    member.addProperty("revision", 0);
                    member.addProperty("updated_at", Statics.getDate());
                    member.addProperty("joined_at", Statics.getDate());
                    member.addProperty("role", pm.role.toString());
                    members.add(member);
                }
                response.get("current").getAsJsonArray().get(0).getAsJsonObject().add("members", members);
                response.get("current").getAsJsonArray().get(0).getAsJsonObject().addProperty("created_at", Statics.getDate());
                response.get("current").getAsJsonArray().get(0).getAsJsonObject().addProperty("updated_at", Statics.getDate());
                response.get("current").getAsJsonArray().get(0).getAsJsonObject().addProperty("id",p.id);
                System.out.println(response.toString());
                res.send(response.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        l.addListener("/party/api/v1/:game/parties/:partyId/members/:accountId/meta", Listener.ListenerType.PATCH,(req,res) -> {
            System.out.println(req.toString());
        });
        l.addListener("/party/api/v1/*/parties", Listener.ListenerType.POST,(req,res) -> {
            System.out.println(req.toString());
        });
        l.addListener("/friends/api/v1/:accountId/recent/fortnite", Listener.ListenerType.GET,(req,res) -> res.send("[]"));
        l.addListener("/presence/api/v1/_/:accountId/last-online",Listener.ListenerType.ALL,(req,res) -> res.send("[]"));
        l.addListener("/party/api/v1/Fortnite/parties/:partyId/members/:memberId",Listener.ListenerType.DELETE,(req,res) -> res.sendStatus(Status._204));
    }
}
