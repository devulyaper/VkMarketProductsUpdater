package vkMarketCrud;

import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.market.MarketAlbum;
import com.vk.api.sdk.objects.market.MarketItem;
import com.vk.api.sdk.objects.market.responses.AddResponse;
import com.vk.api.sdk.objects.photos.responses.GetMarketUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.MarketUploadResponse;
import com.vk.api.sdk.objects.photos.responses.SaveMarketPhotoResponse;
import com.vk.api.sdk.objects.responses.VideoUploadResponse;
import com.vk.api.sdk.objects.video.responses.SaveResponse;
import com.vk.api.sdk.objects.video.responses.UploadResponse;
import com.vk.api.sdk.queries.market.MarketAddQuery;
import com.vk.api.sdk.queries.market.MarketEditQuery;
import parsers.DParser;
import parsers.KlParser;
import parsers.KrParser;
import parsers.Parser;
import product.Product;
import properties.PropertiesLoader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VkMarketCRUD {

    private final UserActor userActor = new UserActor(PropertiesLoader.getUserId(), PropertiesLoader.getAccessToken());
    private final VkApiClient vk = new VkApiClient(new HttpTransportClient());
    private int groupId;
    private String catalogName;
    private Parser parser;
    public VkMarketCRUD(String group, boolean isTest){
        switch (group) {
            case "d" -> {
                if (isTest) {
                    this.groupId = PropertiesLoader.getTestGroupId();
                    this.catalogName = PropertiesLoader.getTestCatalog();
                } else {
                    this.groupId = PropertiesLoader.getDGroupId();
                    this.catalogName = PropertiesLoader.getDCatalog();
                }
                this.parser = new DParser();
            }
            case "kr" -> {
                if (isTest) {
                    this.groupId = PropertiesLoader.getTestGroupId();
                    this.catalogName = PropertiesLoader.getTestCatalog();
                } else {
                    this.groupId = PropertiesLoader.getKGroupId();
                    this.catalogName = PropertiesLoader.getKCatalog();
                }
                this.parser = new KrParser();
            }
            case "kl" -> {
                if (isTest) {
                    this.groupId = PropertiesLoader.getTestGroupId();
                    this.catalogName = PropertiesLoader.getTestCatalog();
                } else {
                    this.groupId = PropertiesLoader.getKlGroupId();
                    this.catalogName = PropertiesLoader.getKlCatalog();
                }
                this.parser = new KlParser();
            }
        }
    }

    public void update(){
        List<Product> products = parser.getAllProducts();
        for(Product product : products){
            try {
                Product deserializedProd = deserialize(findFileBySiteId(product.getSiteId()));
                if(deserializedProd == null){
                    System.out.println("The product: " + product.getLink() + " not in the base.");
                    try {
                        loadProduct(product);
                    } catch (Exception ex){
                        ex.printStackTrace();
                        System.out.println("The product: " + product.getLink() + " not uploaded.");
                    }
                } else {
                    if(product.equals(deserializedProd)){
                        System.out.println("The product " + product.getName() + " has not been changed.");
                    } else {
                        try {
                            updateProduct(product, deserializedProd);
                        } catch (Exception e){
                            e.printStackTrace();
                            System.out.println("Something went wrong. The product " + product.getName() + " have not updated.");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("The product with name: " + product.getName() + " was not deserialized." +
                        "Id: " + product.getSiteId() + " Something went wrong");
                e.printStackTrace();
            }
        }
    }
    public void loadProduct(Product product) throws ClientException, ApiException {
        List<Integer> photoIds = getPhotoIds(product);
        MarketAddQuery marketAddQuery= new MarketAddQuery(vk, userActor, groupId * -1, product.getName(), product.getDescription(), product.getCategoryId());
        marketAddQuery.url(product.getLink());
        if(photoIds.size() > 0){
            marketAddQuery.mainPhotoId(photoIds.get(0));
            marketAddQuery.photoIds(photoIds.subList(1, photoIds.size()));
        }
        if(product.getPrice() != 0){
            marketAddQuery.price(product.getPrice());
        }
        marketAddQuery.oldPrice(product.getOldPrice());
        marketAddQuery.sku(product.getScu());
        marketAddQuery.url(product.getLink());
        if(product.getVideoUrl() != null){
            marketAddQuery.unsafeParam("video_ids", getVideoId(product));
        }
        AddResponse addResponse = marketAddQuery.execute();
        product.setVKId(addResponse.getMarketItemId().toString());
        addProductToAlbum(product);
        serialize(product, "src/main/resources/" + catalogName + "/serialized/" + product.getSiteId());
        System.out.println("the product " + product.getName() + " uploaded");
    }

    public List<Integer> getPhotoIds(Product product) {
        String mainImgUrl = product.getMainImgUrl();
        List<String> imgUrls = product.getImgUrls();
        List<Integer> photoIds = new ArrayList<>();
        try {
            if(mainImgUrl != null){
                photoIds.add(getPhotoId(mainImgUrl, "main",true));
            }
            if(imgUrls != null){
                for(int i = 0; i < (Math.min(imgUrls.size(), 5)); i ++){
                    try {
                        photoIds.add(getPhotoId(imgUrls.get(i), String.valueOf(i), false));
                    } catch (Exception e){
                        System.out.println("Something wend wrong. The additional picture with index " + i +
                                " not loaded to market. Photo url: " + imgUrls.get(i) + " Product link: " +
                                product.getLink());
                        imgUrls.remove(i);
                        product.setImgUrls(imgUrls);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("The main picture " + mainImgUrl + " not loaded to market. " +
                    "The program will try to load additional photo as main." + " Product link: " +
                    product.getLink());
            e.printStackTrace();
            if(imgUrls != null && !imgUrls.isEmpty()){
                product.setMainImgUrl(imgUrls.get(0));
                imgUrls.remove(0);
                product.setImgUrls(imgUrls);
                return getPhotoIds(product);
            }
        }
        return photoIds;
    }

    private Integer getPhotoId(String url, String name, boolean isMain) throws Exception {
        File photo = savePhotoFile(url, name);
        GetMarketUploadServerResponse response = vk.photos().getMarketUploadServer(userActor, groupId)
                .mainPhoto(isMain).execute();
        MarketUploadResponse uploadResponse = vk.upload()
                .photoMarket(response.getUploadUrl().toString(), photo)
                .execute();
        SaveMarketPhotoResponse saveMarketPhotoResponse = vk.photos()
                .saveMarketPhoto(userActor, uploadResponse.getPhoto(), uploadResponse.getServer(),
                        uploadResponse.getHash())
                .cropData(uploadResponse.getCropData())
                .cropHash(uploadResponse.getCropHash())
                .groupId(groupId)
                .execute().get(0);
        Thread.sleep(400);
        return saveMarketPhotoResponse.getId();
    }

    public Integer getVideoId(Product product) {
        try{
            System.out.println(product.getVideoUrl());
            SaveResponse response = vk.videos().save(userActor).name(product.getName())
                    .description(product.getDescription()).isPrivate(true)
                    .link(product.getVideoUrl()).groupId(groupId).execute();
            Thread.sleep(350);
            String resp = null;
            for (int i = 0; i < 5; i++) {
                resp = vk.upload().video(response.getUploadUrl().toString(), null).executeAsString();
                if(resp.contains("\"response\":1")){
                    break;
                }
                Thread.sleep(5000);
            }
            Thread.sleep(5000);
            if(!resp.contains("\"response\":1")){
                product.setVideoUrl("error");
            }
            System.out.println(resp);
            System.out.println(response.getVideoId());
            return response.getVideoId();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public void deleteAllProducts() {
        for(MarketItem marketItem : getAllMarketItems()){
            try {
                int id = marketItem.getId();
                vk.market().delete(userActor, groupId * -1, id).execute();
                System.out.println("The product " + marketItem.getTitle() + " removed.");
                Thread.sleep(500);
                deleteSerializedObjects();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public void deleteBySiteId(String id) {
        try{
            File file = findFileBySiteId(id);
            Product product = deserialize(file);
            assert product != null;
            String vkId = product.getVKId();
            vk.market().delete(userActor, groupId * -1, Integer.parseInt(vkId)).execute();
            deleteSerializedObject(file);
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("The product with id: " + id + " was not deserialized. Something went wrong");
        }
    }

    private void addProductToAlbum(Product product) {
        Integer[] productIds = new Integer[1];
        productIds[0] = Integer.parseInt(product.getVKId());
        try{
            vk.market().addToAlbum(userActor, groupId * -1, productIds, product.getAlbumId()).execute();
        } catch (ApiException | ClientException e){
            e.printStackTrace();
        }
    }
    private void updateProduct(Product product, Product deserializedProduct) throws ClientException,
            ApiException, InterruptedException {
        MarketEditQuery marketEditQuery = new MarketEditQuery(vk, userActor, groupId * -1,
                Integer.parseInt(deserializedProduct.getVKId()), product.getName(), product.getDescription(), product.getCategoryId());
        if(product.getPrice() > 0){
            marketEditQuery.price(product.getPrice());
        }
        if(product.getOldPrice() > 0){
            marketEditQuery.unsafeParam("old_price", product.getOldPrice());
        }
        if(!product.getMainImgUrl().equals(deserializedProduct.getMainImgUrl()) ||
                !product.getImgUrls().equals(deserializedProduct.getImgUrls())) {
            List<Integer> photoIds = getPhotoIds(product);
            if (photoIds.size() > 0) {
                marketEditQuery.mainPhotoId(photoIds.get(0));
                marketEditQuery.photoIds(photoIds.subList(1, photoIds.size()));
            }
        }
        if(product.getVideoUrl() != null && !product.getVideoUrl().equals(deserializedProduct.getVideoUrl())){
            marketEditQuery.unsafeParam("video_ids", getVideoId(product));
        }
        marketEditQuery.execute();
        System.out.println("The product " + product.getName() + " updated.");
        product.setVKId(deserializedProduct.getVKId());
        serialize(product, "src/main/resources/" + catalogName + "/serialized/" + product.getSiteId());
        Thread.sleep(400);
    }
    private List<MarketItem> getAllMarketItems(){
        List<MarketItem> marketItems = new ArrayList<>();
        try {
            marketItems = vk.market().get(userActor, groupId * -1).execute().getItems();
            if(marketItems.size() >= 100){
                marketItems.addAll(vk.market().get(userActor, groupId * -1)
                        .offset(100)
                        .execute()
                        .getItems());
            }
            Thread.sleep(400);
        } catch (Exception e){
            e.printStackTrace();
        }
        return marketItems;
    }

    private void deleteSerializedObjects(){
        File objectsCatalog = new File("src/main/resources/" + catalogName + "/serialized");
        File[] listFiles = objectsCatalog.listFiles();
        if(listFiles != null){
            for(File objectFile : listFiles){
                if(objectFile.delete()){
                    System.out.println("The file " + objectFile.getName() + " removed.");
                } else {
                    System.out.println("Something went wrong. The file " + objectFile.getName() + "not removed.");
                }
            }
        }
    }
    public void printAlbumIds() {
        List<MarketAlbum> albums;
        try{
            albums = vk.market().getAlbums(userActor, groupId * -1).execute().getItems();
            if(albums != null){
                for(MarketAlbum album : albums){
                    System.out.println(album.getTitle() + ": " + album.getId());
                }
            }
        } catch (ApiException | ClientException e){
            e.printStackTrace();
        }
    }
    private File savePhotoFile(String url, String name){
        String filePath = "src/main/resources/photos/" + name + ".jpg";
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }
        try {
            HttpURLConnection urlConn = (HttpURLConnection) new URL(url).openConnection();
            urlConn.setRequestMethod("GET");
            urlConn.connect();
            InputStream in = urlConn.getInputStream();
            Path path = Path.of(filePath);
            Files.copy(in, path);
            urlConn.disconnect();
            System.out.println("img file " + filePath + " have been saved");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
    private void serialize(Product product, String path) {
        try(FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(product);
            System.out.println("Product " + product.getName() + " serialized.");
        } catch (IOException e){
            System.out.println("Something went wrong. Product " + product.getName() + " not serialized.");
            e.printStackTrace();
        }
    }
    private Product deserialize(File file) throws IOException, ClassNotFoundException {
        if(file == null){
            return null;
        } else {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Product product = (Product) ois.readObject();
            fis.close();
            ois.close();
            return product;
        }
    }
    private File findFileBySiteId(String id){
        String path = "src/main/resources/" + catalogName + "/serialized/" + id;
        File file = new File(path);
        if(file.exists()){
            return file;
        } else return null;
    }
    private void deleteSerializedObject(File file) {
        if(file != null){
            if(file.delete()){
                System.out.println("The file " + file.getAbsolutePath() + " was deleted.");
            } else {
                System.out.println("The file " + file.getAbsolutePath() + " was not deleted.");
            }
        }
    }
}
