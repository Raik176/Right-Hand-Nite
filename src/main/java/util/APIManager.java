package util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import express.utils.MediaType;
import express.utils.Status;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class APIManager extends Manager {
    public APIManager(Listener l) {
        super(l);
        final String key = "ccabee93-99ac8721-351c3885-290fb213";
        new AutoAPIManager(() -> {
            if (Statics.config != null && !Statics.config.get("shop").getAsJsonObject().get("updateAuto").getAsBoolean()) return;
            try {
                URLConnection con = new URL("https://api.nitestats.com/v1/epic/store").openConnection();
                con.connect();
                Gson g = new GsonBuilder().setPrettyPrinting().create();
                JsonObject shop = g.fromJson(new BufferedReader(new InputStreamReader(con.getInputStream())).readLine(),JsonObject.class);
                for (JsonElement storefront : shop.get("storefronts").getAsJsonArray()) {
                    for (JsonElement catalogEntry : storefront.getAsJsonObject().get("catalogEntries").getAsJsonArray()) {
                        for (JsonElement price : catalogEntry.getAsJsonObject().get("prices").getAsJsonArray()) {
                            JsonObject p = price.getAsJsonObject();
                            p.addProperty("regularPrice",0);
                            p.addProperty("finalPrice",0);
                            p.addProperty("basePrice",0);
                            p.addProperty("currencyType","MtxCurrency");
                        }
                        if (catalogEntry.getAsJsonObject().has("dynamicBundleInfo")) {
                            JsonObject dbi = catalogEntry.getAsJsonObject().get("dynamicBundleInfo").getAsJsonObject();
                            dbi.addProperty("discountedBasePrice",0);
                            dbi.addProperty("regularBasePrice",0);
                            dbi.addProperty("floorPrice",0);
                            dbi.addProperty("currencyType","MtxCurrency");
                            for (JsonElement bundleItem : dbi.get("bundleItems").getAsJsonArray()) {
                                JsonObject item = bundleItem.getAsJsonObject();
                                item.addProperty("regularPrice",0);
                                item.addProperty("discountedPrice",0);
                                item.addProperty("alreadyOwnedPriceReduction",0);

                            }
                        }
                    }
                }
                int customItems = 1;
                JsonObject storeFront = new JsonObject();
                storeFront.addProperty("name","BRSpecialRHN");
                JsonArray storeFrontItems = new JsonArray();
                for (JsonElement item : Statics.config.get("shop").getAsJsonObject().get("customShopItems").getAsJsonArray()) {
                    JsonObject i = new JsonObject();
                    i.addProperty("devName", "[VIRTUAL] 1x Custom Shop Item " + customItems + " for 0 MtxCurrency");
                    i.addProperty("offerId","v2:/" + UUID.randomUUID().toString());
                    i.add("fullfillmentIds",new JsonArray());
                    i.addProperty("dailyLimit",-1);
                    i.addProperty("weeklyLimit",-1);
                    i.addProperty("monthlyLimit",-1);
                    i.add("categories",new JsonArray());
                    JsonObject price = new JsonObject();
                    price.addProperty("currencyType","MtxCurrency");
                    price.addProperty("currencySubType","");
                    price.addProperty("regularPrice",0);
                    price.addProperty("dynamicRegularPrice",0);
                    price.addProperty("finalPrice",0);
                    price.addProperty("saleExpiration","9999-12-31T23:59:59.999Z");
                    price.addProperty("basePrice",0);
                    JsonArray prices = new JsonArray();
                    prices.add(price);
                    i.add("prices",prices);
                    i.add("meta",item.getAsJsonObject().get("meta").getAsJsonObject());
                    i.addProperty("matchFilter","");
                    i.addProperty("filterWeight",0.0f);
                    JsonArray reqs = new JsonArray();
                    for (JsonElement je : item.getAsJsonObject().get("grants").getAsJsonArray()) {
                        JsonObject req = new JsonObject();
                        req.addProperty("requirementType","DenyOnItemOwnership");
                        req.addProperty("requiredId",je.getAsString());
                        req.addProperty("minQuantity",1);
                        reqs.add(req);
                    }
                    i.add("requirements",reqs);
                    JsonObject giftInfo = new JsonObject();
                    giftInfo.addProperty("bIsEnabled",true);
                    giftInfo.addProperty("forcedGiftBoxTemplateId","");
                    giftInfo.add("purchaseRequirements",new JsonArray());
                    giftInfo.add("giftRecordIds",new JsonArray());
                    i.add("giftInfo",giftInfo);
                    i.addProperty("refundable",true);
                    JsonArray metaInfos = new JsonArray();
                    String actualId = item.getAsJsonObject().get("meta").getAsJsonObject().get("NewDisplayAssetPath").getAsString();
                    for (String k : item.getAsJsonObject().get("meta").getAsJsonObject().keySet()) {
                        JsonObject metaInfo = new JsonObject();
                        metaInfo.addProperty("key",k);
                        metaInfo.addProperty("value",item.getAsJsonObject().get("meta").getAsJsonObject().get(k).getAsString());
                        if (k.equalsIgnoreCase("NewDisplayAssetPath")) {
                            metaInfo.addProperty("value",actualId);
                        }
                        metaInfos.add(metaInfo);
                    }
                    item.getAsJsonObject().get("meta").getAsJsonObject().addProperty("NewDisplayAssetPath",actualId);
                    i.add("metaInfo",metaInfos);
                    if (item.getAsJsonObject().has("isBundle") && item.getAsJsonObject().get("isBundle").getAsBoolean()) {
                        JsonObject dbi = new JsonObject();
                        dbi.addProperty("discountedBasePrice",0);
                        dbi.addProperty("regularBasePrice",0);
                        dbi.addProperty("floorPrice",0);
                        dbi.addProperty("curencyType","MtxCurrency");
                        dbi.addProperty("currencySubType","");
                        dbi.addProperty("displayType","AmountOff");
                        JsonArray bundleItems = new JsonArray();
                        for (JsonElement je : item.getAsJsonObject().get("grants").getAsJsonArray()) {
                            JsonObject bundleItem = new JsonObject();
                            bundleItem.addProperty("bCanOwnMultiple",false);
                            bundleItem.addProperty("regularPrice",0);
                            bundleItem.addProperty("discountedPrice",0);
                            bundleItem.addProperty("alreadyOwnedPriceReduction",0);
                            JsonObject itm = new JsonObject();
                            itm.addProperty("templateId",je.getAsString());
                            itm.addProperty("quantity",1);
                            bundleItem.add("item",itm);
                            bundleItems.add(bundleItem);
                        }
                        dbi.add("bundleItems",bundleItems);
                        i.add("dynamicBundleInfo",dbi);
                        i.addProperty("offerType","DynamicBundle");
                    } else {
                        i.addProperty("offerType","StaticPrice");
                    }
                    JsonArray itemGrants = new JsonArray();
                    for (JsonElement je : item.getAsJsonObject().get("grants").getAsJsonArray()) {
                        JsonObject itemGrant = new JsonObject();
                        itemGrant.addProperty("templateId",je.getAsString());
                        itemGrant.addProperty("quantity",1);
                        itemGrants.add(itemGrant);
                    }
                    i.add("itemGrants",itemGrants);
                    customItems++;
                    storeFrontItems.add(i);
                }
                storeFront.add("catalogEntries",storeFrontItems);
                shop.get("storefronts").getAsJsonArray().add(storeFront);
                File f = new File("jsons/shop.json");
                FileWriter fw = new FileWriter(f);
                fw.write(g.toJson(shop));
                fw.flush();
                fw.close();
            } catch (Exception e) {
                Statics.warn("Couldn't update the shop. Please try to restart the server. If the problem persists contact the owners of the backend!");
                e.printStackTrace();
            }
        });
        /*new AutoAPIManager(() -> {
            try {
                Gson g = new GsonBuilder().setPrettyPrinting().create();
                HttpURLConnection con = (HttpURLConnection) new URL("https://fortniteapi.io/v2/challenges?season=current&lang=en").openConnection();
                con.setRequestProperty("Authorization",key);
                con.connect();
                Statics.bundles = new Gson().fromJson(new BufferedReader(new InputStreamReader(con.getInputStream())).readLine(),JsonObject.class).get("bundles").getAsJsonArray();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        new AutoAPIManager(() -> {
            try {
                Gson g = new GsonBuilder().setPrettyPrinting().create();
                HttpURLConnection con = (HttpURLConnection) new URL("https://fortniteapi.io/v2/battlepass?season=current&lang=en").openConnection();
                con.setRequestProperty("Authorization",key);
                con.connect();
                String out = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
                File f = new File("jsons/battlepass.json");
                f.createNewFile();
                FileWriter fw = new FileWriter(f);
                fw.write(g.toJson(g.fromJson(out,JsonObject.class)));
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/
        Statics.bundles = new JsonArray();
        new AutoAPIManager(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL("https://fortnite-api.com/v2/cosmetics/br").openConnection();
                con.connect();
                JsonArray res = new Gson().fromJson(new BufferedReader(new InputStreamReader(con.getInputStream())).readLine(),JsonObject.class).get("data").getAsJsonArray();
                JsonObject items = new JsonObject();
                for (JsonElement je : res) {
                    JsonObject data = je.getAsJsonObject();
                    JsonObject item = new JsonObject();
                    String id = data.get("type").getAsJsonObject().get("backendValue").getAsString() + ":" + data.get("id").getAsString();
                    if (id.startsWith("BannerToken")) continue;
                    item.addProperty("templateId", id);
                    JsonObject attributes = new JsonObject();
                    attributes.addProperty("max_level_bonus",0);
                    attributes.addProperty("level",1);
                    attributes.addProperty("item_seen",false);
                    attributes.addProperty("xp",0);
                    JsonArray variants = new JsonArray();
                    if (!data.get("variants").isJsonNull()) {
                        for (JsonElement je2 : data.get("variants").getAsJsonArray()) {
                            JsonObject data2 = je2.getAsJsonObject();
                            JsonObject variant = new JsonObject();
                            variant.addProperty("channel",data2.get("channel").getAsString());
                            JsonArray options = data2.get("options").getAsJsonArray();
                            variant.addProperty("active",options.get(0).getAsJsonObject().get("tag").getAsString());
                            JsonArray owned = new JsonArray();
                            options.forEach(o -> owned.add(o.getAsJsonObject().get("tag").getAsString()));
                            variant.add("owned",owned);
                            variants.add(variant);
                        }
                    }
                    attributes.add("variants",variants);
                    attributes.addProperty("creation_time",Statics.getDate());
                    attributes.addProperty("favorite",false);
                    item.add("attributes",attributes);
                    item.addProperty("quantity",1);
                    UUID uuid;
                    while (items.has((uuid = UUID.randomUUID()).toString()));
                    items.add(uuid.toString(),item);
                }
                Statics.items = items;
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void listen(Listener l) {
        l.addListener("/fortnite/api/v2/versioncheck/:platform",Listener.ListenerType.ALL,(req,res) -> res.send("{\"type\":\"NO_UPDATE\"}"));
        l.addListener("/waitingroom/api/waitingroom",Listener.ListenerType.GET,(req,res) -> {res.setStatus(Status._204); res.send("");});
        l.addListener("/eulatracking/api/public/agreements/fn/account/:accountId", Listener.ListenerType.GET,(req,res) -> res.sendStatus(Status._204));
        l.addListener("/lightswitch/api/service/bulk/status", Listener.ListenerType.GET,(req,res) -> res.send("[{\"serviceInstanceId\":\"" + "Fortnite" + "\",\"status\":\"UP\",\"message\":\"Hello\",\"maintenanceUri\":\"https://dsc.gg/neonite\",\"allowedActions\":[],\"banned\":false,\"launcherInfoDTO\":{\"appName\":\"Fortnite\",\"catalogItemId\":\"4fe75bbc5a674f4f9b356b5c90567da5\",\"namespace\":\"fn\"}}]"));
        l.addListener("/fortnite/api/game/v2/enabled_features", Listener.ListenerType.GET,(req,res) -> res.send("[]"));
        l.addListener("/fortnite/api/storefront/v2/keychain", Listener.ListenerType.GET, (req,res) -> {
            try {
                res.setContentType(MediaType._json);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL("https://api.nitestats.com/v1/epic/keychain").openConnection().getInputStream()));
                res.send(bufferedReader.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        l.addListener("/fortnite/api/receipts/v1/account/:accountId/receipts", Listener.ListenerType.GET,(req,res) -> {
            res.send("[{\"appStore\":\"EpicPurchasingService\",\"appStoreId\":\"000000000000000\",\"receiptId\":\"3b2317a2aab94ec5ad0a3a22affdee98\",\"receiptInfo\":\"ENTITLEMENT\"}]");
        });
        l.addListener("/socialban/api/public/v1/:accountId", Listener.ListenerType.GET, (req,res) -> res.sendStatus(Status._204));
        l.addListener("/friends/api/v1/:accountId/summary", Listener.ListenerType.GET, (req,res) -> res.send("{\"friends\":{},\"incoming\":[],\"suggested\":[],\"blocklist\":[],\"settings\":{\"acceptInvites\":\"public\"},\"limitsReached\":{\"incoming\":false,\"outgoing\":false,\"accepted\":false}}"));
        l.addListener("/fortnite/api/storefront/v2/catalog", Listener.ListenerType.GET, (req,res) -> {
            res.setContentType(MediaType._json);
            res.send(Statics.readJson("jsons/shop.json").toString());
        });
        l.addListener("/statsproxy.json/api/statsv2/account/:accountId", Listener.ListenerType.GET, (req,res) -> res.send(MessageFormat.format("{\"startTime\":0,\"endTime\":9223372036854776000,\"stats\":{},\"accountId\":\"{0}\"}",req.get("accountid").getAsString())));
        l.addListener("/content/api/pages/fortnite-game/", Listener.ListenerType.GET, (req,res) -> {
            try {
                URLConnection con = new URL("https://fortnitecontent-website-prod07.ol.epicgames.com/content/api/pages/fortnite-game/").openConnection();
                con.connect();
                Gson g = new Gson();
                JsonObject content = g.fromJson(new BufferedReader(new InputStreamReader(con.getInputStream())).readLine(),JsonObject.class);
                content.add("emergencynoticev2",g.fromJson("{\"jcr:isCheckedOut\":true,\"_title\":\"emergencynoticev2\",\"_noIndex\":false,\"emergencynotices\":{\"_type\":\"Emergency Notices\",\"emergencynotices\":[{\"hidden\":false,\"_type\":\"CommonUI Emergency Notice Base\",\"title\":\"Right Hand Nite\",\"body\":\"Developer Build\"}]},\"_activeDate\":\"2018-08-06T19:00:26.217Z\",\"lastModified\":\"2021-03-17T15:07:27.924Z\",\"_locale\":\"en-US\"}",JsonObject.class));
                content.add("dynamicbackgrounds",g.fromJson("{\"jcr:isCheckedOut\":false,\"backgrounds\":{\"backgrounds\":[{\"backgroundimage\":\"http://127.0.0.1:5595/client/bgs/flipped.png\",\"stage\":\"defaultnotris\",\"_type\":\"DynamicBackground\",\"key\":\"lobby\"},{\"backgroundimage\":\"http://127.0.0.1:5595/client/bgs/" + Statics.config.get("shop").getAsJsonObject().get("background").getAsString() + "\",\"stage\":\"defaultnotris\",\"_type\":\"DynamicBackground\",\"key\":\"vault\"}],\"_type\":\"DynamicBackgroundList\"},\"_title\":\"dynamicbackgrounds\",\"_noIndex\":false,\"jcr:baseVersion\":\"a7ca237317f1e70712af90-59fe-4576-8f32-f80bf513c946\",\"_activeDate\":\"2020-07-06T06:00:00.000Z\",\"lastModified\":\"2021-06-22T13:53:48.402Z\",\"_locale\":\"en-US\"}",JsonObject.class));
                JsonObject gameInfo = content.get("subgameinfo").getAsJsonObject();
                gameInfo.get("battleroyale").getAsJsonObject().addProperty("description","Neonite Java Experience");
                gameInfo.get("creative").getAsJsonObject().addProperty("description","Not supported!");
                gameInfo.get("savetheworld").getAsJsonObject().addProperty("description","Not supported!");
                JsonArray sections = content.get("shopSections").getAsJsonObject().get("sectionList").getAsJsonObject().get("sections").getAsJsonArray();
                for (JsonElement je : Statics.config.get("shop").getAsJsonObject().get("customSections").getAsJsonArray()) {
                    JsonObject cs = je.getAsJsonObject();
                    JsonObject section = new JsonObject();
                    JsonArray bgs = new JsonArray();
                    JsonObject background = new JsonObject();
                    background.addProperty("stage","defaultnotris");
                    background.addProperty("_type","DynamicBackground");
                    background.addProperty("key","vault");
                    background.addProperty("backgroundimage","http://127.0.0.1:5595/client/bgs/" + cs.get("background").getAsString());
                    bgs.add(background);
                    section.addProperty("bSortOffersByOwnership", false);
                    section.addProperty("bShowIneligibleOffersIfGiftable", false);
                    section.addProperty("bEnableToastNotification", true);
                    section.add("backgrounds",bgs);
                    section.addProperty("_type","ShopSection");
                    section.addProperty("landingPriority",cs.get("priority").getAsInt());
                    section.addProperty("bHidden",false);
                    section.addProperty("sectionId",cs.get("id").getAsString());
                    section.addProperty("bShowTimer",cs.get("showTimer").getAsBoolean());
                    section.addProperty("sectionDisplayName",cs.get("displayName").getAsString());
                    section.addProperty("bShowIneligibleOffers",true);
                    sections.add(section);
                }
                content.add("subgameinfo",gameInfo);
                JsonArray news = Statics.get(new URL("https://fortnite-api.com/v2/news/br")).getAsJsonObject().get("data").getAsJsonObject().get("motds").getAsJsonArray();
                for (int i=0;i<news.size();i++) {
                    JsonObject o = news.get(i).getAsJsonObject();
                    o.addProperty("_type","CommonUI Simple Message MOTD");
                    o.addProperty("entryType","Text");
                    o.addProperty("isSeasonLaunchFlow",true);
                    if (o.get("title").isJsonNull()) news.remove(o);
                }
                content.get("battleroyalenewsv2").getAsJsonObject().get("news").getAsJsonObject().add("motds",news);
                res.send(content.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        l.addListener("/fortnite/api/game/v2/br-inventory/account/:accountId",Listener.ListenerType.GET, (req,res) -> {
            res.setContentType(MediaType._json);
            res.send("{\"stash\":{\"globalcash\":5000}}");
        });
        l.addListener("/fortnite/api/calendar/v1/timeline",Listener.ListenerType.GET,(req,res) -> {
            res.setContentType(MediaType._json);
            JsonObject response = new Gson().fromJson("{\"channels\":{\"standalone-store\":{\"states\":[{\"validFrom\":\"2022-01-31T08:31:11.349Z\",\"activeEvents\":[],\"state\":{\"activePurchaseLimitingEventIds\":[],\"storefront\":{},\"rmtPromotionConfig\":[],\"storeEnd\":\"0001-01-01T00:00:00.000Z\"}}],\"cacheExpire\":\"2022-01-31T10:31:11.349Z\"},\"client-matchmaking\":{\"states\":[{\"validFrom\":\"2022-01-31T08:31:11.349Z\",\"activeEvents\":[],\"state\":{\"region\":{\"BR\":{\"eventFlagsForcedOff\":[\"Playlist_DefaultDuo\"]}}}}],\"cacheExpire\":\"2022-01-31T10:31:11.349Z\"},\"tk\":{\"states\":[{\"validFrom\":\"2022-01-31T08:31:11.349Z\",\"activeEvents\":[],\"state\":{\"k\":[\"1162D72490AB6D040106B276D14B20D2:UoybfdmMPLyXmLpTKBZB0sqSOvXnKHabF/nv5olkuoQ=\",\"E66DF3CF1BFE84F0B1966967210DD6D9:DGLD/iFbdLvaiZnAfWrHIW5yJ5SfsQQyjeW2IBQe+zw=\",\"B4BE2A5487426AFF06CB7089EA9B75BE:w7Hbt5PsQ4MDg9EjvOU8WdtL/BrWxZNp9NM2UrHSjRg=\",\"2648ACDF6B7E55495928F2319101BB8A:tKha+iiFKUamRIWCxq0gOtbN/G1B5J5eOIElAx9T3rc=\",\"89CD763ACF4C3672A0F74AC0F45C291F:vtcPbnTh2aDOt7LfYpJL+7aF1TaB8LDaLsMoQ47NZec=\",\"E098A699B1A5E20B03B5CBBCDB85D4E3:oKYx1AqjUax4YhKirSQDeyBSQNkSmEKDS7q+U/4KszU=\"]}}],\"cacheExpire\":\"2022-01-31T10:31:11.349Z\"},\"featured-islands\":{\"states\":[{\"validFrom\":\"2022-01-31T08:31:11.349Z\",\"activeEvents\":[],\"state\":{\"islandCodes\":[\"6289-9797-1760?v=18\",null,null,\"2732-4374-8886?v=55\"],\"playlistCuratedContent\":{},\"playlistCuratedHub\":{\"Playlist_PlaygroundV2\":\"4142-8437-4091\",\"Playlist_Creative_PlayOnly\":\"4142-8437-4091\"},\"islandTemplates\":[]}}],\"cacheExpire\":\"2022-01-31T10:31:11.349Z\"},\"community-votes\":{\"states\":[{\"validFrom\":\"2022-01-31T08:31:11.349Z\",\"activeEvents\":[],\"state\":{\"electionId\":\"\",\"candidates\":[],\"electionEnds\":\"9999-12-31T23:59:59.999Z\",\"numWinners\":1}}],\"cacheExpire\":\"2022-01-31T10:31:11.349Z\"},\"client-events\":{\"states\":[{\"validFrom\":\"2022-01-31T08:31:11.349Z\",\"activeEvents\":[{\"eventType\":\"EventFlag.BBPromo.Quests\",\"activeUntil\":\"2090-02-14T00:00:00.000Z\",\"activeSince\":\"2019-11-19T00:00:00.000Z\"},{\"eventType\":\"WL0\",\"activeUntil\":\"2022-09-14T07:00:00.000Z\",\"activeSince\":\"2020-08-01T07:00:00.000Z\"},{\"eventType\":\"EventFlag.Event_TheMarch\",\"activeUntil\":\"2022-08-30T13:00:00.000Z\",\"activeSince\":\"2021-08-26T13:00:00.000Z\"},{\"eventType\":\"EventFlag.LobbySeason{0}\",\"activeUntil\":\"2022-03-29T13:00:00.000Z\",\"activeSince\":\"2021-12-02T14:00:00.000Z\"},{\"eventType\":\"EventFlag.LobbyStW.NewBeginnings\",\"activeUntil\":\"2022-02-08T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.PassiveIceStorms\",\"activeUntil\":\"2022-03-14T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.PassiveFireStorms\",\"activeUntil\":\"2022-03-14T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.PassiveLightningStorms\",\"activeUntil\":\"2022-03-14T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.ActiveMiniBosses\",\"activeUntil\":\"2022-03-14T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.MissionAlert.MegaAlert\",\"activeUntil\":\"2022-03-14T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.MissionAlert.MegaAlertMiniboss\",\"activeUntil\":\"2022-03-14T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.Season12.NoDancing.Quests\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.Phoenix.NewBeginnings\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.ElderGroupMissions\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.Phoenix.NewBeginnings.Quests\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.LoveStorm.EnableEnemyVariants\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.Wargames.Start\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"EventFlag.Outpost\",\"activeUntil\":\"2022-04-06T00:00:00.000Z\",\"activeSince\":\"2022-01-25T00:00:00.000Z\"},{\"eventType\":\"c_2021_SM09\",\"activeUntil\":\"2022-02-01T09:00:00.000Z\",\"activeSince\":\"2022-01-28T09:00:00.000Z\"}],\"state\":{\"activeStorefronts\":[],\"eventNamedWeights\":{},\"activeEvents\":[{\"instanceId\":\"42e4qrq8b28d9jlqovnglg8d25[2]0\",\"devName\":\"Event_TheMarch\",\"eventName\":\"CalendarEvent_TheMarch\",\"eventStart\":\"2021-08-26T13:00:00Z\",\"eventEnd\":\"2022-08-30T13:00:00Z\",\"eventType\":\"EventFlag.Event_TheMarch\"}],\"seasonNumber\":{0},\"seasonTemplateId\":\"AthenaSeason:athenaseason{0}\",\"matchXpBonusPoints\":0,\"eventPunchCardTemplateId\":\"\",\"seasonBegin\":\"2021-12-02T14:00:00Z\",\"seasonEnd\":\"2022-03-29T13:00:00Z\",\"seasonDisplayedEnd\":\"2022-03-15T04:00:00Z\",\"weeklyStoreEnd\":\"2022-02-01T00:00:00Z\",\"stwEventStoreEnd\":\"2022-04-04T00:00:00.000Z\",\"stwWeeklyStoreEnd\":\"2022-02-03T00:00:00.000Z\",\"sectionStoreEnds\":{\"Featured\":\"2022-02-01T00:00:00Z\",\"Featured2\":\"2022-02-01T00:00:00Z\",\"Daily\":\"2022-02-01T00:00:00Z\",\"Rabbit\":\"2022-02-01T00:00:00Z\",\"Goalbound\":\"2022-02-01T00:00:00Z\",\"Goalbound2\":\"2022-02-01T00:00:00Z\",\"GreenGoblinB\":\"2022-02-01T00:00:00Z\",\"Special2\":\"2022-02-01T00:00:00Z\",\"Special3\":\"2022-02-01T00:00:00Z\",\"ViB\":\"2022-02-01T00:00:00Z\",\"JinxB\":\"2022-02-01T00:00:00Z\",\"Special\":\"2022-02-01T00:00:00Z\"},\"storeRefreshEventEndDate\":\"0001-01-01T00:00:00.000Z\",\"rmtPromotion\":\"\",\"dailyStoreEnd\":\"2022-02-01T00:00:00Z\"}}],\"cacheExpire\":\"2022-01-31T10:31:11.349Z\"}},\"cacheIntervalMins\":15,\"currentTime\":\"2022-01-31T08:55:41.589Z\"}".replaceAll("\\{0}",req.get("user_agent").getAsString().split("-")[1].split("\\.")[0]),JsonObject.class);
            res.send(response.toString());
        });
        l.addListener("/fortnite/api/game/v2/privacy/account/:accountId", Listener.ListenerType.GET,(req,res) -> res.send("{\"accountId\":\"{id\",\"optOutOfPublicLeaderboards\":false"));
        l.addListener("/prod/:file",Listener.ListenerType.GET,(req,res) -> {
            try {
                res.setContentType(MediaType._jpeg);
                BufferedImage img = ImageIO.read(new URL("https://cdn-live.prm.ol.epicgames.com/prod/" + req.get("file").getAsString() + "?width=" + req.get("width").getAsInt()));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img,"jpeg",baos);
                res.sendBytes(baos.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        l.addListener("/api/v1/user/setting",Listener.ListenerType.POST,(req,res) -> {
            res.setContentType(MediaType._json);
            String id = req.get("body").getAsJsonObject().get("accountId").getAsString();
            JsonObject profileData = Statics.readJson("accounts/"+id+"/profiles/profile_athena.json");
            JsonArray response = new JsonArray();
            JsonObject avatar = new JsonObject();
            avatar.addProperty("accountId",id);
            avatar.addProperty("key","avatar");
            avatar.addProperty("value",Statics.readJson("accounts/"+id+"/profiles/profile_athena.json").get("items").getAsJsonObject().get("sandbox_loadout").getAsJsonObject().get("attributes").getAsJsonObject().get("locker_slots_data").getAsJsonObject().get("slots").getAsJsonObject().get("Character").getAsJsonObject().get("items").getAsJsonArray().get(0).getAsString());
            response.add(avatar);
            JsonObject avatarBackground = new JsonObject();
            avatarBackground.addProperty("accountId",id);
            avatarBackground.addProperty("key","avatarBackground");
            avatarBackground.addProperty("value","[\"#B4F2FE\",\"#00ACF2\",\"#005679\"]");
            response.add(avatarBackground);
            JsonObject appInstalled = new JsonObject();
            appInstalled.addProperty("accountId",id);
            appInstalled.addProperty("key","appInstalled");
            appInstalled.addProperty("value","init");
            response.add(appInstalled);
            res.send(response.toString());
        });
        l.addListener("/launcher/api/public/assets/:platform/:hash/FortniteContentBuilds",Listener.ListenerType.GET,(req,res) -> {
            res.send("{\"appName\":\"FortniteContentBuilds\",\"labelName\":\"zH2NzecPy1fFg0Gj50C3tc3tIo3nZA-Windows\",\"buildVersion\":\"++Fortnite+Release-20.00-CL-19381079-Windows\",\"catalogItemId\":\"5cb97847cee34581afdbc445400e2f77\",\"expires\":\"2022-03-20T18:27:34.571Z\",\"items\":{\"MANIFEST\":{\"signature\":\"Policy=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6IipCdWlsZHMvRm9ydG5pdGUvQ29udGVudC9DbG91ZERpci9lNjgzX3RzRm9XVFVGT2FUSkV5RldTX0t1TkxkcXcubWFuaWZlc3QiLCJDb25kaXRpb24iOnsiRGF0ZUxlc3NUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE2NDc4MDA4NTR9LCJJcEFkZHJlc3MiOnsiQVdTOlNvdXJjZUlwIjoiMC4wLjAuMC8wIn19fV19&Signature=VkuS-4ig8CfNhLJG746EudaTrYed6WbDjeowrGrEKRzjD97b1TEKsmnaUoDQqtZPzb3vASYyByjDieiFre62qEa8RzRGJ-qSMGJ6OjAA68Q0zyAwmZJm5wKRbGiNI1HB42Jk~sWBgdl3T3cmUdV-R1zBUHcp9lKPWbMbmRccb71D0namtzx2Es1kHfJWnhpslnCTUPJL27uwVL8tRbLeX-L~LA~xnDkJkOpdwdRIBJGg1r8Gz-WKAtGT~UbNear0ZwIKv7ezozQexiw~Sh9W91N5S~9WEg6cIGr7XDsGq5Pynt8BotHtnjZv5lsVR2atHtyYRB4ol1aFA3m2w9Slpg__&Key-Pair-Id=APKAI5CNFPJPTPYZISXQ\",\"distribution\":\"https://download.epicgames.com/\",\"path\":\"Builds/Fortnite/Content/CloudDir/e683_tsFoWTUFOaTJEyFWS_KuNLdqw.manifest\",\"hash\":\"e44de3cfebd565007fbbcdb406a8da1aa9f68eb9\",\"additionalDistributions\":[\"https://download2.epicgames.com/\",\"https://download3.epicgames.com/\",\"https://download4.epicgames.com/\"]},\"CHUNKS\":{\"signature\":\"Policy=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6IipCdWlsZHMvRm9ydG5pdGUvQ29udGVudC9DbG91ZERpci9lNjgzX3RzRm9XVFVGT2FUSkV5RldTX0t1TkxkcXcubWFuaWZlc3QiLCJDb25kaXRpb24iOnsiRGF0ZUxlc3NUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE2NDc4MDA4NTR9LCJJcEFkZHJlc3MiOnsiQVdTOlNvdXJjZUlwIjoiMC4wLjAuMC8wIn19fV19&Signature=VkuS-4ig8CfNhLJG746EudaTrYed6WbDjeowrGrEKRzjD97b1TEKsmnaUoDQqtZPzb3vASYyByjDieiFre62qEa8RzRGJ-qSMGJ6OjAA68Q0zyAwmZJm5wKRbGiNI1HB42Jk~sWBgdl3T3cmUdV-R1zBUHcp9lKPWbMbmRccb71D0namtzx2Es1kHfJWnhpslnCTUPJL27uwVL8tRbLeX-L~LA~xnDkJkOpdwdRIBJGg1r8Gz-WKAtGT~UbNear0ZwIKv7ezozQexiw~Sh9W91N5S~9WEg6cIGr7XDsGq5Pynt8BotHtnjZv5lsVR2atHtyYRB4ol1aFA3m2w9Slpg__&Key-Pair-Id=APKAI5CNFPJPTPYZISXQ\",\"distribution\":\"https://download.epicgames.com/\",\"path\":\"Builds/Fortnite/Content/CloudDir/e683_tsFoWTUFOaTJEyFWS_KuNLdqw.manifest\",\"additionalDistributions\":[\"https://download2.epicgames.com/\",\"https://download3.epicgames.com/\",\"https://download4.epicgames.com/\"]}},\"assetId\":\"FortniteContentBuilds\"}");
        });
        l.addListener("/launcher/api/public/distributionpoints/", Listener.ListenerType.GET,(req,res) -> {
            res.send("{\"distributions\":[\"https://download.epicgames.com/\",\"https://download2.epicgames.com/\",\"https://download3.epicgames.com/\",\"https://download4.epicgames.com/\",\"https://epicgames-download1.akamaized.net/\",\"https://fastly-download.epicgames.com/\"]}");
        });
    }
}
