package parsers;

import dHtmlGenerator.ApartmentPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import product.Product;
import properties.PropertiesLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DParser implements Parser {

    private String dDomain = PropertiesLoader.getDDomain();
    private String dCatalog = PropertiesLoader.getDSiteCatalog();

    @Override
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        for(String link : parseAllProductLinks()){
            products.add(parseProductLink(link));
        }
        return products;
    }

    public List<String> parseAllProductLinks() {
        List<String> links = new ArrayList<>();
        try{
            Document doc = getDocument(dCatalog);
            Elements elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                    "p > a");
            for(Element element : elements){
                String link = element.attr("href");
                if(!link.contains(dDomain)){
                    link = dDomain + link;
                }
                links.add(link);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return links;
    }

    public Product parseProductLink(String link) {
        Product product = new Product();
        try{
            Document document = getDocument(link);
            product.setLink(link);
            String name = getProductName(document);
            product.setName(name);
            product.setDescription(getProductDescription(document, link));
            product.setCategoryId(500);
            product.setMainImgUrl(getMainImgUrl(document));
            product.setImgUrls(getImgUrls(document));
            product.setPrice(getPrice(document));
            product.setSiteId(getSiteId(name));
            product.setAlbumId(getAlbumId(name));
            product.setVideoUrl(getVideoUrl(document));
        } catch (Exception e){
            System.out.println("The parsing of product link: " + link + " failed.");
            e.printStackTrace();
            return null;
        }
        return product;
    }

    public String getProductName(Document doc){
        String siteName = doc.select("#content > div:nth-child(1) > div > div > div.heading-block > h1").text();
        return siteName.replace("КВАРТИРА", "КВ.");
    }

    private String getProductDescription(Document doc, String link) {
        Elements priceElements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                "ul:nth-child(2) > li");
        if(priceElements.isEmpty()){
            priceElements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > div > " +
                    "ul > li");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Стоимость размещения:\n");
        for(Element el : priceElements){
            stringBuilder.append("\n");
            stringBuilder.append(el.text());
        }
        String remote = doc.select("#content > div:nth-child(1) > div > div > " +
                "div.t300 > div > p").text();
        if(!remote.isEmpty()){
            stringBuilder.append("\n\n");
            stringBuilder.append("В этой квартире доступно удалённое заселение! " +
                    "Теперь вам не нужно оформлять договор в нашем офисе перед заселением. " +
                    "Это можно сделать через мессенджер, а ключи будут находиться в боксе возле квартиры. ");
        }
        Elements pElements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > p");
        for(Element pElement : pElements){
            if(pElement.text().isEmpty()){
                continue;
            }
            stringBuilder.append("\n\n");
            if(pElement.text().startsWith("В этой квартире доступно удалённое заселение")){
                stringBuilder.append("В этой квартире доступно удалённое заселение! " +
                        "Теперь вам не нужно оформлять договор в нашем офисе перед заселением. " +
                        "Это можно сделать через мессенджер, а ключи будут находиться в боксе возле квартиры. ");
                continue;
            }
            stringBuilder.append(pElement.text());
        }
        stringBuilder.append("\n\nКвартира от гостиницы Декабрист. Чита, ул. Забайкальского Рабочего 45.");
        return stringBuilder.toString();
    }

    private String getMainImgUrl(Document doc) {
        Elements elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                "figure > img");
        if(elements.isEmpty()){
            elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > div > " +
                    "figure > img");
        }
        if(elements.isEmpty()){
            elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > center > " +
                    "figure > img");
        }
        String link = elements.get(0).attr("src");
        if(!link.startsWith(dDomain)) link = dDomain + link;
        return link;
    }

    private List<String> getImgUrls(Document doc) {
        List<String> photoSources = new ArrayList<>();
        Elements elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                "figure > img");
        if(elements.isEmpty()){
            elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > div > " +
                    "figure > img");
        }
        if(elements.isEmpty()){
            elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > center > " +
                    "figure > img");
        }
        int photosCount = (Math.min(elements.size(), 5));
        for(int i = 1; i < photosCount; i ++){
            String src = elements.get(i).attr("src");
            if(!src.startsWith(dDomain)) src = dDomain + src;
            photoSources.add(src);
        }
        return photoSources;
    }

    private double getPrice(Document doc) {
        String priceWithLetters = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                "div > ul > li:nth-child(2) > span").text();
        if(priceWithLetters.isEmpty()){
            priceWithLetters = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                    "ul:nth-child(2) > li:nth-child(2) > span").text();
        }
        if(priceWithLetters.isEmpty()){
            priceWithLetters = doc.select("#content > div > div:nth-child(1) > div > div.t300 > " +
                    "div > div > ul > li:nth-child(2) > span").text();
        }
        String price = getOnlyNumbers(priceWithLetters);
        return Double.parseDouble(price);
    }

    private String getSiteId(String name) {
        Pattern pattern = Pattern.compile("[№][\\s]*[\\d]*");
        Matcher matcher = pattern.matcher(name);
        String result = null;
        while(matcher.find()){
            result = matcher.group();
        }
        assert result != null;
        return getOnlyNumbers(result);
    }

    int getAlbumId(String name) {
        if(name.startsWith("1")){
            return 4;
        } else if(name.startsWith("2")){
            return 5;
        } else if(name.startsWith("3") || name.startsWith("4") || name.startsWith("5")){
            return 6;
        } else {
            System.out.println("apartment " + name + " does not start with suitable character. " +
                    "Can't get album id.");
            throw new RuntimeException();
        }
    }

    public String getOnlyNumbers(String priceText){
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < priceText.length(); i ++){
            String charr = priceText.substring(i, i + 1);
            if(charr.matches("\\d")){
                result.append(charr);
            }
        }
        return result.toString();
    }

    public Document getDocument(String link) throws IOException {
        return Jsoup.connect(link).get();
    }

    public Double getHalfPrice(Document doc) {
        String halfPrice = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                "div > ul > li:nth-child(1) > span").text();
        if(halfPrice.isEmpty()){
            halfPrice = doc.select("#content > div:nth-child(1) > div > div > div.t300 > " +
                    "ul:nth-child(2) > li:nth-child(1) > span").text();
        }
        if(halfPrice.isEmpty()){
            halfPrice = doc.select("#content > div > div:nth-child(1) > div > div.t300 > " +
                    "div > div > ul > li:nth-child(1) > span").text();
        }
        return Double.parseDouble(getOnlyNumbers(halfPrice));
    }

    public List<Product> getProducts(ApartmentPage page) throws IOException {
        List<Product> products = new ArrayList<>();
        Document document = getDocument(PropertiesLoader.getDSiteCatalog());
        Elements catalogApartments = document.select("#content > div:nth-child(1) > div > div > div.t300 > p > a");
        String roomsPage = "студия";
        switch (page){
            case ONE_ROOM_APARTMENT -> roomsPage = "1";
            case TWO_ROOM_APARTMENT -> roomsPage = "2";
            case THREE_OR_MORE_APARTMENT -> roomsPage = "3";
        }
        for(Element element : catalogApartments){
            String elText = element.text();
            String rooms = elText.substring(elText.lastIndexOf("(") + 1, elText.lastIndexOf(")"))
                    .split("-")[0].trim();
            String catalogRooms = rooms;
            if(catalogRooms.equals("4")){
                catalogRooms = "3";
            }
            if(catalogRooms.equals(roomsPage)){
                String link = element.attr("href");
                if(!link.contains(dDomain)){
                    link = dDomain + link;
                }
                Product product = parseProductLink(link);
                String amountOfRoomsFromPage = Character.toString(product.getName().charAt(0));
                if(page == ApartmentPage.STUDIO && amountOfRoomsFromPage.equals("1")){
                    amountOfRoomsFromPage = "студия";
                }
                if(!amountOfRoomsFromPage.equals(rooms)){
                    System.out.println(amountOfRoomsFromPage + " != " + rooms);
                    System.out.println("The amount of apartment rooms is different in catalog and in its page. " +
                            "Apartment link: " + link);
                    throw new RuntimeException();
                }
                products.add(parseProductLink(link));
            }
        }
        return products;
    }

    public String getVideoUrl(Document doc){
        Element el = doc.select("p > iframe").last();
        if(el != null){
            String result = "https://youtu.be/";
            String[] docSegments = el.attr("src").split("/");
            return result + docSegments[docSegments.length - 1];
        } else {
            return null;
        }
    }

    public int getEmptyNumber() throws IOException {
        Document doc = getDocument(dCatalog);
        Elements elements = doc.select("#content > div:nth-child(1) > div > div > div.t300 > p > a");
        for(int i = 0; i < elements.size(); i ++){
            String pElement = elements.get(i).text();
            int num = Integer.parseInt(getSiteId(pElement));
            if(num != i + 1){
                return i + 1;
            }
        }
        return elements.size() + 1;
    }
}
