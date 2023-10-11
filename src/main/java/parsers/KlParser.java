package parsers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import product.Product;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KlParser implements Parser{

    public List<Product> getAllProducts(){
        List<Product> products = new ArrayList<>();
        int totalProductsAmount = getTotalProductsAmount();
        JSONObject jsonProductsObject = getJSONProductsSlice(totalProductsAmount);
        for(Object object : jsonProductsObject.getJSONArray("products")){
            try{
                JSONObject jsonProductObject = (JSONObject) object;
                Product product = getProduct(jsonProductObject);
                products.add(0, product);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return products;
    }

    private Product getProduct(JSONObject jsonProductObject) {
        Product product = new Product();
        product.setName(getProductName(jsonProductObject));
        product.setDescription(getDescription(jsonProductObject));
        try{
            product.setPrice(getPrice(jsonProductObject));
        } catch (Exception e){
            product.setPrice(0);
        }
        product.setLink(getLink(jsonProductObject));
        product.setSiteId(getSiteId(jsonProductObject));
        product.setCategoryId(600);
        List<String> allImgUrls = getImgUrls(jsonProductObject);
        product.setMainImgUrl(allImgUrls.get(0));
        List<String> imgUrls = new ArrayList<String>(allImgUrls.subList(1, allImgUrls.size()));
        product.setImgUrls(imgUrls);
        return product;
    }

    private int getTotalProductsAmount() {
        JSONObject object = getJSONProductsSlice(1);
        return object.getInt("total");
    }

    public JSONObject getJSONProductsSlice(int productsAmount) {
        try {
            URL url = new URL("https://store.tildacdn.com/api/getproductslist/?storepartuid=220154419311&recid=608372198&c=1693734717463&getparts=false&getoptions=false&slice=1&size=" + productsAmount);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));

            String output;
            StringBuilder builder = new StringBuilder();
            while ((output = br.readLine()) != null) {
                String outString = new String(output.getBytes("windows-1251"), "UTF-8");
                builder.append(outString);
            }
            connection.disconnect();
            return new JSONObject(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> getImgUrls(JSONObject jsonProductObject) {
        List<String> urls = new ArrayList<>();
        String imgLinks = jsonProductObject.getString("gallery");
        for(Object img : new JSONArray(imgLinks)){
            JSONObject imgObject = (JSONObject) img;
            urls.add(imgObject.getString("img"));
        }
        return urls;
    }

    public String getSiteId(JSONObject jsonProductObject) {
        return jsonProductObject.get("uid").toString();
    }

    public String getLink(JSONObject jsonProductObject) {
        return jsonProductObject.getString("url");
    }

    public String getProductName(JSONObject jsonObject){
        return replaceBrokenChars(jsonObject.getString("title"));
    }

    public String getDescription(JSONObject jsonObject){
        Document doc = Jsoup.parse(jsonObject.getString("text"));
        StringBuilder stringBuilder = new StringBuilder();
        for(String str : doc.html().split("<br>")){
            stringBuilder.append("\n");
            if(str.isEmpty()){
                continue;
            }
            Document strDoc = Jsoup.parse("<body>" + str + "</body>");
            for(Element el : strDoc.select("p")){
                stringBuilder.append(el.text() + "\n");
            }
            for(int i = 0; i < strDoc.select("ul > li").size(); i ++){
                stringBuilder.append("- " + strDoc.select("ul > li").get(i).text() + "\n");
            }

            stringBuilder.append(strDoc.body().ownText());
        }
        JSONArray characteristics = (JSONArray) jsonObject.get("characteristics");
        String description = replaceBrokenChars(stringBuilder.toString().trim());
        try {
            description += "\n\nИнвертор: " + ((JSONObject)characteristics.get(0)).get("value");
        } catch (Exception e){
            description += "\n";
        }
        try {
            description += "\nГарантия: " + ((JSONObject) characteristics.get(1)).get("value");
        } catch (Exception ignored){}

        description += "\n\nСтоимость: ";
        for(Object object : (JSONArray) jsonObject.get("editions")){
            JSONObject editionObject = (JSONObject) object;
            String price = editionObject.getString("price");
            try{
                String power = editionObject.getString("Хладопроизводительность, кВт");
                if(price.isEmpty()){
                    description += ("\nХладопроизводительность " + power + " кВт - " + "Цена по запросу.");
                } else {
                    description += ("\nХладопроизводительность " + power + " кВт - " + price + " руб.");
                }
            } catch (Exception e){
                description += price + " руб.";
            }

        }
        return description;
    }


    public double getPrice(JSONObject jsonObject) {
        return Double.parseDouble(jsonObject.get("price").toString());
    }

    public String replaceBrokenChars(String string){
        char[] chars = string.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < chars.length; i ++){
            char ch = chars[i];
            if(Arrays.equals(String.valueOf(ch).getBytes(StandardCharsets.UTF_8), new byte[]{-17, -65,-67})){
                if(i != chars.length-1 && (byte) chars[i+1] == 63){
                    stringBuilder.append('И');
                    continue;
                }
            }
            if((byte) ch == 63 && i != 0){
                if(Arrays.equals(String.valueOf(chars[i-1]).getBytes(StandardCharsets.UTF_8), new byte[]{-17, -65,-67})){
                    continue;
                }
            }
            stringBuilder.append(ch);
        }
        return stringBuilder.toString();
    }

}
