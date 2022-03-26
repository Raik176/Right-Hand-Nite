package util;

import com.google.gson.*;
import express.utils.MediaType;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MCPManager extends Manager {
    public MCPManager(Listener l) {
        super(l);
    }

    public JsonObject getOrCreateProfile(String profileId, String accountId) {
        File f = new File("accounts\\" + accountId + "\\profiles");
        if (!f.exists()) {
            f.mkdirs();
            File templates = new File("profile_templates");
            try {
                for (File template : templates.listFiles()) {
                    File file = new File(f.toPath().toString(),"\\" + template.getName());
                    Files.copy(template.toPath(),file.toPath());
                    StringBuilder out = new StringBuilder();
                    String line;
                    BufferedReader br = new BufferedReader(new FileReader(template));
                    while ((line = br.readLine()) != null) {
                        out.append(line);
                    }
                    JsonObject o = new Gson().fromJson(out.toString(),JsonObject.class);
                    o.addProperty("_id",accountId);
                    o.addProperty("accountId",accountId);
                    o.addProperty("created",Statics.getDate());
                    o.addProperty("updated",Statics.getDate());
                    FileWriter fw = new FileWriter(file);
                    fw.write(new GsonBuilder().setPrettyPrinting().create().toJson(o));
                    fw.flush();
                }
            } catch (Exception e) {e.printStackTrace();}
        }
        try {
            return new Gson().fromJson(Files.readString(Path.of(f.toPath().toString(),"\\","profile_" + profileId + ".json")),JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void listen(Listener l) {
        AtomicReference<JsonArray> items = new AtomicReference<>(new JsonArray());
        try {
            JsonObject cosmetics = Statics.get(new URL("https://fortnite-api.com/v2/cosmetics/br")).getAsJsonObject();
            for (JsonElement cosmetic : cosmetics.get("data").getAsJsonArray()) {
                JsonObject c = new JsonObject();
                c.addProperty("templateId",cosmetic.getAsJsonObject().get("type").getAsJsonObject().get("backendValue").getAsString() + ":" + cosmetic.getAsJsonObject().get("id").getAsString());
                c.addProperty("quantity",1);
                items.get().add(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        l.addListener("/fortnite/api/game/v2/profile/:accountId/client/:command", Listener.ListenerType.POST, (req,res) -> {
            res.setContentType(MediaType._json);
            try {
                int rvn = -1;
                boolean saveProfile = true;
                if (req.has("rvn")) {
                    rvn = req.get("rvn").getAsInt();
                }
                String id = req.get("accountId").getAsString();
                String profileId = req.get("profileId").getAsString();
                JsonObject profileData = getOrCreateProfile(profileId, id);
                JsonArray profileChanges = new JsonArray();
                JsonArray multiUpdate = new JsonArray();
                JsonArray notifications = new JsonArray();
                switch (req.get("command").getAsString()) {
                    case "ClientQuestLogin":
                        break;
                    case "CopyCosmeticLoadout":
                        System.out.println(req);
                        JsonArray loadouts = profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().get("loadouts").getAsJsonArray();
                        String copyTo = "sandbox_loadout_"+req.get("body").getAsJsonObject().get("targetIndex").getAsInt();
                        String copyFrom = loadouts.get(req.get("body").getAsJsonObject().get("sourceIndex").getAsInt()).getAsString();
                        System.out.println(loadouts.size());
                        if (loadouts.size()>req.get("body").getAsJsonObject().get("targetIndex").getAsInt()) {
                            loadouts.set(req.get("body").getAsJsonObject().get("targetIndex").getAsInt(),new JsonPrimitive(copyTo));
                        } else {
                            loadouts.add(copyTo);
                        }
                        JsonObject loadout = profileData.get("items").getAsJsonObject().get(copyFrom).getAsJsonObject().deepCopy();
                        loadout.get("attributes").getAsJsonObject().addProperty("locker_name",req.get("body").getAsJsonObject().get("optNewNameForTarget").getAsString());
                        profileData.get("items").getAsJsonObject().add(copyTo,loadout);
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "SetCosmeticLockerName":
                        profileData.get("items").getAsJsonObject().get(req.get("body").getAsJsonObject().get("lockerItem").getAsString()).getAsJsonObject().get("attributes").getAsJsonObject().addProperty("locker_name",req.get("body").getAsJsonObject().get("name").getAsString());
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "DeleteCosmeticLoadout":
                        String name = profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().get("loadouts").getAsJsonArray().get(req.get("body").getAsJsonObject().get("index").getAsInt()).getAsString();
                        profileData.get("items").getAsJsonObject().remove(name);
                        profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().addProperty("last_applied_loadout","sandbox_layout");
                        profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().addProperty("active_loadout_index",0);
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "PurchaseCatalogEntry":
                        profileId = "athena";
                        profileData = getOrCreateProfile(profileId,id);
                        JsonObject shop = Statics.readJson("jsons/shop.json");
                        JsonObject correctEntry = null;
                        for (JsonElement storefront : shop.get("storefronts").getAsJsonArray()) {
                            for (JsonElement catalogEntry : storefront.getAsJsonObject().get("catalogEntries").getAsJsonArray()) {
                                if (catalogEntry.getAsJsonObject().get("offerId").getAsString().equalsIgnoreCase(req.get("body").getAsJsonObject().get("offerId").getAsString())) {
                                    correctEntry = catalogEntry.getAsJsonObject();
                                }
                            }
                        }
                        if (correctEntry != null) {
                            JsonArray lootResults = new JsonArray();
                            for (JsonElement grant : correctEntry.get("itemGrants").getAsJsonArray()) {
                                JsonObject lootResult = new JsonObject();
                                lootResult.addProperty("itemType",grant.getAsJsonObject().get("templateId").getAsString());
                                lootResult.addProperty("itemGuid", UUID.randomUUID().toString());
                                lootResult.addProperty("itemProfile","athena");
                                lootResult.addProperty("quantity",grant.getAsJsonObject().get("quantity").getAsInt());
                                lootResults.add(lootResult);

                                JsonObject profileItem = new JsonObject();
                                profileItem.addProperty("templateId",grant.getAsJsonObject().get("templateId").getAsString());
                                JsonObject attributes = new JsonObject();
                                attributes.addProperty("max_level_bonus",0);
                                attributes.addProperty("level",1);
                                attributes.addProperty("item_seen",false);
                                attributes.addProperty("xp",0);
                                attributes.add("variants",new JsonArray());
                                attributes.addProperty("creation_time",Statics.getDate());
                                attributes.addProperty("favorite",false);
                                profileItem.add("attributes",attributes);
                                profileItem.addProperty("quantity",grant.getAsJsonObject().get("quantity").getAsInt());
                                profileData.get("items").getAsJsonObject().add(lootResult.get("itemGuid").getAsString(),profileItem);
                            }
                            int vbucks = 0;
                            for (JsonElement metaInfo : correctEntry.get("metaInfo").getAsJsonArray()) {
                                JsonElement k = metaInfo.getAsJsonObject().get("key");
                                String key = k != null ? k.getAsString() : metaInfo.getAsJsonObject().get("Key").getAsString();
                                if (key.equalsIgnoreCase("MtxQuantity") || key.equalsIgnoreCase("MtxBonus")) {
                                    vbucks += Integer.parseInt(metaInfo.getAsJsonObject().get("value").getAsString());
                                }
                            }
                            if (vbucks != 0) {
                                profileId = "common_core";
                                profileData = getOrCreateProfile(profileId,id);
                                JsonObject lootResult = new JsonObject();
                                lootResult.addProperty("itemType","Currency:MtxPurchased");
                                lootResult.addProperty("itemGuid", UUID.randomUUID().toString());
                                lootResult.addProperty("itemProfile","common_core");
                                lootResult.addProperty("quantity",vbucks);
                                lootResults.add(lootResult);

                                JsonObject profileItem = new JsonObject();
                                profileItem.addProperty("templateId","Currency:MtxPurchased");
                                JsonObject attributes = new JsonObject();
                                attributes.addProperty("platform","EpicPC");
                                profileItem.add("attributes",attributes);
                                profileItem.addProperty("quantity",vbucks);
                                profileData.get("items").getAsJsonObject().add(lootResult.get("itemGuid").getAsString(),profileItem);
                                JsonObject obj = new JsonObject();
                                obj.addProperty("changeType","fullProfileUpdate");
                                obj.add("profile",profileData);
                                profileChanges.add(obj);
                            }
                            JsonObject catalogPurchase = new JsonObject();
                            catalogPurchase.addProperty("type","CatalogPurchase");
                            catalogPurchase.addProperty("primary",true);
                            JsonObject lootResult = new JsonObject();
                            lootResult.add("items",lootResults);
                            catalogPurchase.add("lootResult",lootResult);
                            notifications.add(catalogPurchase);
                            profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        }
                        break;
                    case "ExchangeGameCurrencyForBattlePassOffer":
                        HashMap<String,JsonObject> data = new HashMap<>();
                        for (JsonElement je : Statics.readJson("jsons/battlepass.json").get("battlepass_item_list").getAsJsonArray()) {
                            data.put(je.getAsJsonObject().get("offerId").getAsString(),je.getAsJsonObject());
                        }
                        for (JsonElement item : req.get("body").getAsJsonObject().get("offerItemIdList").getAsJsonArray()) {
                            if (data.containsKey(item.getAsString())) {
                                for (JsonElement lr : data.get(item.getAsString()).get("lootResult").getAsJsonArray()) {
                                    JsonObject tempProfile = getOrCreateProfile(lr.getAsJsonObject().get("itemProfile").getAsString(),id);
                                    JsonObject profileItem = new JsonObject();
                                    profileItem.addProperty("templateId",lr.getAsJsonObject().get("itemType").getAsString());
                                    JsonObject attributes = new JsonObject();
                                    attributes.addProperty("max_level_bonus",0);
                                    attributes.addProperty("level",1);
                                    attributes.addProperty("item_seen",false);
                                    attributes.addProperty("xp",0);
                                    attributes.add("variants",new JsonArray());
                                    attributes.addProperty("creation_time",Statics.getDate());
                                    attributes.addProperty("favorite",false);
                                    profileItem.add("attributes",attributes);
                                    profileItem.addProperty("quantity",lr.getAsJsonObject().get("quantity").getAsInt());
                                    tempProfile.get("items").getAsJsonObject().add(UUID.randomUUID().toString(),profileItem);
                                    JsonObject update = new JsonObject();
                                    update.addProperty("changeType","fullProfileUpdate");
                                    update.add("profile",tempProfile);
                                    multiUpdate.add(update);
                                }
                                profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().addProperty("battlestars",(profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().get("battlestars").getAsInt()-data.get(item.getAsString()).get("totalCurrencyPaid").getAsInt()));
                                System.out.println(req);
                                //profileData.get("stats").getAsJsonObject().get("attributes").getAsJsonObject().get("purchased_bp_offers").getAsJsonArray().add(data.get(item.getAsString()));
                                profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                            }
                        }
                        break;
                    case "SetCosmeticLockerSlot":
                        JsonObject preset = profileData.get("items").getAsJsonObject().get(req.get("body").getAsJsonObject().get("lockerItem").getAsString()).getAsJsonObject();
                        JsonObject lockerData = preset.get("attributes").getAsJsonObject().get("locker_slots_data").getAsJsonObject();
                        JsonObject slot = lockerData.get("slots").getAsJsonObject().get(req.get("body").getAsJsonObject().get("category").getAsString()).getAsJsonObject();
                        int index = req.get("body").getAsJsonObject().get("slotIndex").getAsInt();
                        JsonArray activeVariants = new JsonArray();
                        JsonArray variantUpdates = req.get("body").getAsJsonObject().get("variantUpdates").getAsJsonArray();
                        for (int i=0;i<slot.get("items").getAsJsonArray().size();i++) {
                            JsonObject activeVariant = new JsonObject();
                            JsonObject variant = new JsonObject();
                            if (variantUpdates.size()>i) {
                                variant = variantUpdates.get(i).getAsJsonObject();
                                activeVariant.add("variants",variant);
                            }
                            activeVariants.add(activeVariant);
                        }
                        slot.add("activeVariants",activeVariants);
                        if (index == -1) {
                            for (int i=0;i<slot.get("items").getAsJsonArray().size();i++) {
                                slot.get("items").getAsJsonArray().set(i,req.get("body").getAsJsonObject().get("itemToSlot"));
                                if (!req.get("body").getAsJsonObject().get("variantUpdates").getAsJsonArray().isEmpty()) {
                                    slot.get("activeVariants").getAsJsonArray().get(i).getAsJsonObject().add("variants",new JsonArray());
                                    for (JsonElement je : req.get("body").getAsJsonObject().get("variantUpdates").getAsJsonArray()) {
                                        slot.get("activeVariants").getAsJsonArray().get(i).getAsJsonObject().get("variants").getAsJsonArray().add(je);
                                    }
                                }
                            }
                        } else {
                            slot.get("items").getAsJsonArray().set(index,req.get("body").getAsJsonObject().get("itemToSlot"));
                            if (!req.get("body").getAsJsonObject().get("variantUpdates").getAsJsonArray().isEmpty()) {
                                slot.get("activeVariants").getAsJsonArray().get(0).getAsJsonObject().add("variants",new JsonArray());
                                for (JsonElement je : req.get("body").getAsJsonObject().get("variantUpdates").getAsJsonArray()) {
                                    slot.get("activeVariants").getAsJsonArray().get(0).getAsJsonObject().get("variants").getAsJsonArray().add(je);
                                }
                            }
                        }
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "SetItemFavoriteStatusBatch":
                        Statics.info(req);
                        HashMap<String,Integer> positions = new HashMap<>();
                        items.set(req.get("body").getAsJsonObject().get("itemIds").getAsJsonArray());
                        for (int i = 0; i< items.get().size(); i++) {
                            positions.put(items.get().get(i).getAsString(),i);
                        }
                        for (String guid : profileData.get("items").getAsJsonObject().keySet()) {
                            JsonObject item = profileData.get("items").getAsJsonObject().get(guid).getAsJsonObject();
                            if (positions.containsKey(item.get("templateId").getAsString())) {
                                item.get("attributes").getAsJsonObject().addProperty("favorite",req.get("body").getAsJsonObject().get("itemFavStatus").getAsJsonArray().get(positions.get(item.get("templateId").getAsString())).getAsBoolean());
                            }
                        }
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "MarkItemSeen":
                        List<String> itemsToMark = new ArrayList<>();
                        for (JsonElement itemId : req.get("body").getAsJsonObject().get("itemIds").getAsJsonArray()) {
                            itemsToMark.add(itemId.getAsString());
                        }
                        for (String guid : profileData.get("items").getAsJsonObject().keySet()) {
                            JsonObject item = profileData.get("items").getAsJsonObject().get(guid).getAsJsonObject();
                            if (itemsToMark.contains(item.get("templateId").getAsString())) {
                                item.get("attributes").getAsJsonObject().addProperty("item_seen",true);
                            }
                        }
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "SetCosmeticLockerBanner":
                        preset = profileData.get("items").getAsJsonObject().get(req.get("body").getAsJsonObject().get("lockerItem").getAsString()).getAsJsonObject();
                        preset.addProperty("banner_icon_template",req.get("body").getAsJsonObject().get("bannerIconTemplateName").getAsString().toLowerCase());
                        preset.addProperty("banner_color_template",req.get("body").getAsJsonObject().get("bannerColorTemplateName").getAsString().toLowerCase());
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                    case "RemoveGiftBox":
                        for (JsonElement jsonElement : req.get("body").getAsJsonObject().get("giftBoxItemIds").getAsJsonArray()) {
                            JsonArray lootResult = profileData.get("items").getAsJsonObject().get(jsonElement.getAsString()).getAsJsonObject().get("attributes").getAsJsonObject().get("lootList").getAsJsonArray();
                            profileData.get("items").getAsJsonObject().remove(jsonElement.getAsString());
                            JsonObject athena = getOrCreateProfile("athena",id);
                            for (JsonElement i : lootResult) {
                                JsonObject item = i.getAsJsonObject();
                                JsonObject aItem = new JsonObject();
                                aItem.addProperty("templateId",item.get("itemType").getAsString());
                                JsonObject attributes = new JsonObject();
                                attributes.addProperty("max_level_bonus",0);
                                attributes.addProperty("level",1);
                                attributes.addProperty("item_seen",false);
                                attributes.addProperty("xp",0);
                                attributes.add("variants", new JsonArray());
                                attributes.addProperty("creation_time",Statics.getDate());
                                attributes.addProperty("favorite",false);
                                aItem.add("attributes",attributes);
                                aItem.addProperty("quantity",1);
                                athena.add(item.get("itemGuid").getAsString(),aItem);
                            }
                            JsonObject multi = new JsonObject();
                            JsonArray changes = new JsonArray();
                            JsonObject change = new JsonObject();
                            change.addProperty("changeType","fullProfileUpdate");
                            change.add("profile",athena);
                            changes.add(change);
                            multi.add("profileChanges",changes);
                            multiUpdate.add(multi);
                        }
                        profileData.addProperty("rvn",(profileData.get("rvn").getAsInt()+1));
                        break;
                }
                Statics.log("COMMAND " + req.get("command").getAsString());

                if (rvn != profileData.get("rvn").getAsInt()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("changeType","fullProfileUpdate");
                    obj.add("profile",profileData);
                    profileChanges.add(obj);
                }

                if (profileId.equals("athena")) {
                    List<String> ids = new ArrayList<>();
                    for (String key : profileData.get("items").getAsJsonObject().keySet()) ids.add(profileData.get("items").getAsJsonObject().get(key).getAsJsonObject().get("templateId").getAsString());
                    for (String s : Statics.items.keySet().stream().filter(s -> !ids.contains(Statics.items.get(s).getAsJsonObject().get("templateId").getAsString())).collect(Collectors.toList())) profileData.get("items").getAsJsonObject().add(s,Statics.items.get(s));
                    JsonArray grantedBundles = new JsonArray();
                    for (JsonElement b : Statics.bundles) {
                        String bundleId;
                        while (profileData.get("items").getAsJsonObject().has(bundleId = UUID.randomUUID().toString()));
                        if (!ids.contains("ChallengeBundle:" + b.getAsJsonObject().get("id").getAsString())) {
                            JsonObject bundle = new JsonObject();
                            bundle.addProperty("templateId","ChallengeBundle:" + b.getAsJsonObject().get("id").getAsString());
                            JsonObject attr = new JsonObject();
                            attr.addProperty("has_unlock_by_completion",false);
                            attr.addProperty("num_quests_completed",0);
                            attr.addProperty("level",0);
                            JsonArray grantsQuests = new JsonArray();
                            for (JsonElement q : b.getAsJsonObject().get("quests").getAsJsonArray()) {
                                if (!ids.contains("Quest:" + q.getAsJsonObject().get("id").getAsString())) {
                                    String uuid;
                                    while (profileData.get("items").getAsJsonObject().has(uuid = UUID.randomUUID().toString()));
                                    profileData.get("items").getAsJsonObject().add(uuid,new Gson().fromJson("{\"templateId\":\"" + "Quest:" + q.getAsJsonObject().get("id").getAsString() + "\",\"attributes\":{\"creation_time\":\"" + Statics.getDate() + "\",\"level\":-1,\"item_seen\":false,\"playlists\":[],\"sent_new_notification\":true,\"challenge_bundle_id\":\"" + bundleId + "\",\"xp_reward_scalar\":1,\"challenge_linked_quest_given\":\"\",\"quest_pool\":\"\",\"quest_state\":\"Active\",\"bucket\":\"\",\"last_state_change_time\":\"" + Statics.getDate() + "\",\"challenge_linked_quest_parent\":\"\",\"max_level_bonus\":0,\"completion_" + q.getAsJsonObject().get("id").getAsString().toLowerCase() + "_obj0\":\"0\",\"xp\":0,\"quest_rarity\":\"common\",\"favorite\":false},\"quantity\":1}",JsonObject.class));
                                    grantsQuests.add(uuid);
                                }
                            }
                            attr.add("grantedquestinstanceids",grantsQuests);
                            attr.addProperty("item_seen",false);
                            attr.addProperty("max_allowed_bundle_level",0);
                            attr.addProperty("num_granted_bundle_quests",grantsQuests.size());
                            attr.addProperty("max_level_bonus",0);
                            attr.addProperty("num_progress_quests_completed",0);
                            attr.addProperty("xp",0);
                            attr.addProperty("favorite",false);
                            bundle.add("attributes",attr);
                            bundle.addProperty("quantity",1);
                            profileData.get("items").getAsJsonObject().add(bundleId,bundle);
                            grantedBundles.add(bundleId);
                        }
                        if (!ids.contains("ChallengeBundleSchedule:right_hand_bundle_schedule")) {
                            JsonObject schedule = new JsonObject();
                            schedule.addProperty("templateId","ChallengeBundleSchedule:right_hand_bundle_schedule");
                            JsonObject atr = new JsonObject();
                            atr.addProperty("unlock_epoch","0000-00-00T00:00:00.000Z");
                            atr.addProperty("max_level_bonus",0);
                            atr.addProperty("level",1);
                            atr.addProperty("item_seen",false);
                            atr.addProperty("xp",0);
                            atr.addProperty("favorite",false);
                            atr.add("granted_bundles",grantedBundles);
                            schedule.add("attributes",atr);
                            schedule.addProperty("quantity",1);
                            String uuid;
                            while (profileData.get("items").getAsJsonObject().has(uuid = UUID.randomUUID().toString()));
                            profileData.get("items").getAsJsonObject().add(uuid,schedule);
                        } else {
                            for (String s : profileData.get("items").getAsJsonObject().keySet()) {
                                JsonObject item = profileData.get("items").getAsJsonObject().get(s).getAsJsonObject();
                                if (item.get("templateId").getAsString().equalsIgnoreCase("ChallengeBundleSchedule:right_hand_bundle_schedule")) {
                                    JsonArray bundles = item.get("attributes").getAsJsonObject().get("granted_bundles").getAsJsonArray();
                                    for (JsonElement je : grantedBundles) {
                                        bundles.add(je);
                                    }
                                    item.get("attributes").getAsJsonObject().add("granted_bundles",bundles);
                                    break;
                                }
                            }
                        }
                    }
                }

                JsonObject response = new JsonObject();
                response.addProperty("profileRevision", profileData.get("rvn").getAsInt());
                response.addProperty("profileId", profileId);
                response.addProperty("profileChangesBaseRevision", profileData.get("rvn").getAsInt());
                response.add("profileChanges", profileChanges);
                response.add("notifications",notifications);
                response.addProperty("serverTime", "2022-01-21T20:28:30.545Z");
                response.addProperty("profileCommandRevision", profileData.get("commandRevision") != null ? profileData.get("commandRevision").getAsInt() : 1);
                response.addProperty("responseVersion", 1);
                response.add("multiUpdate",multiUpdate);

                File f = new File("accounts/" + id + "/profiles/profile_" + profileId + ".json");
                FileWriter fw = new FileWriter(f);
                fw.write(new GsonBuilder().setPrettyPrinting().create().toJson(profileData));
                fw.flush();
                fw.close();

                res.send(response.toString());
            } catch (Exception e) {
                Statics.error("Couldn't handle that request, please try to restart the server. If the problem persists conact the owners of this backend!");
                e.printStackTrace();
            }
        });
    }
}
