package parsers;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import org.jsoup.nodes.Document;
import product.Product;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KrParser implements Parser {

    private final static String KRISTOFER_DOMAIN = "https://kristofer.ru";
    public List<String> getAllProductLinks() {
        List<String> links = new ArrayList<>();
        String path = "src/main/resources/kristofer/KristoferLinks.txt";
        try(BufferedReader reader = new BufferedReader(new FileReader(path))){
            while(reader.ready()){
                String line = reader.readLine();
                if(!line.isEmpty()){
                    links.add(line);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return links;
    }

    public Product getProduct(String link) throws IOException {
        Document document = getDocument(link);
        Product kristoferProduct = new Product();
        kristoferProduct.setLink(link);
        kristoferProduct.setName(getProductName(document));
        kristoferProduct.setDescription(getProductDescriprion(document));
        kristoferProduct.setCategoryId(600);
        kristoferProduct.setMainImgUrl(getMainImgUrl(document));
        kristoferProduct.setImgUrls(getImgUrls(document));
        kristoferProduct.setPrice(getPrice(document));
        kristoferProduct.setOldPrice(getOldPrice(document));
        String scu = getScu(document);
        kristoferProduct.setSiteId(getSiteId(scu));
        kristoferProduct.setScu(scu);
        try {
            kristoferProduct.setAlbumId(getAlbumId(document));
        } catch (RuntimeException ignored){}
        return kristoferProduct;
    }

    private String getScu(Document document) {
        return document.select("body > section.productCardContent > div > div > div > " +
                        "div.productCardContentHeader > div.productCardContentRight > div.itemDesc > span")
                .text().substring(9);
    }

    private double getPrice(Document document) {
        String price = document.select("body > section.productCardContent > div > div > div > " +
                "div.productCardContentHeader > div.productCardContentRight > div.itemPriceBlockWrapper " +
                "> div.itemPriceBlock > div:nth-child(3) > span > span.value").text();
        if(getOldPrice(document) != 0){
            price = document.select("body > section.productCardContent > div > div > div > " +
                    "div.productCardContentHeader > div.productCardContentRight > div.itemPriceBlockWrapper" +
                    " > div.itemPriceBlock > div:nth-child(3) > span > span.itemNewPrice > span.value").text();
        }
        return Double.parseDouble(getOnlyNumbers(price));
    }

    private double getOldPrice(Document document) {
        String oldPrice = document.select("body > section.productCardContent > div > div > div > " +
                "div.productCardContentHeader > div.productCardContentRight > div.itemPriceBlockWrapper > " +
                "div.itemPriceBlock > div:nth-child(3) > span > span.itemOldPrice").text();
        if(oldPrice.isEmpty()){
            return 0;
        } else {
            return Integer.parseInt(getOnlyNumbers(oldPrice));
        }
    }

    private List<String> getImgUrls(Document document) {
        List<String> photoUrls = new ArrayList<>();
        Elements elements = document.select("#productCardSlider > a");
        for(int i = 0; i < Math.min(elements.size(), 4); i ++){
            String photoUrl = elements.get(i).attr("href");
            if(!photoUrl.contains(KRISTOFER_DOMAIN)){
                photoUrl = KRISTOFER_DOMAIN + photoUrl;
            }
            photoUrls.add(photoUrl);
        }
        return photoUrls;
    }

    private String getMainImgUrl(Document document) {
        String photoUrl = document.select("body > section.productCardContent > div > div > div > " +
                "div.productCardContentHeader > div.productCardContentLeft > div.productCardContentImg > " +
                "div > img").attr("src");
        if(!photoUrl.startsWith(KRISTOFER_DOMAIN)){
            photoUrl = KRISTOFER_DOMAIN + photoUrl;
        }
        return photoUrl;
    }

    private int getAlbumId(Document document) {
        String link = document.select("body > section.productCardTop > div > div > div > div > " +
                "span:nth-child(7) > a").attr("href");
        String category = link.split("/")[3];
        switch (category) {
            case "gazonokosilki_i_trimmery": return 10;
            case "stekloochistiteli": return 7;
            case "podmetalnye_mashiny": return 8;
            case "minimoyki": return 9;
            case "tekhnika_dlya_vlazhnoy_uborki_pola": return 6;
            case "nasosy": return 4;
            case "pylesosy": return 2;
            case "sad": return 5;
            case "paroochistiteli": return 3;
            default: {
                System.out.println("Product " + getProductName(document) + " does not start with suitable " +
                        "character. Can't get album id.");
                throw new RuntimeException();
            }
        }
    }

    private String getProductDescriprion(Document document) {
        return document.select("body > section.productCardContent > div > div > div > " +
                "div.productCardContentBody > div.productCardContentTextWrapper.show > div").text();
    }

    private String getProductName(Document document) {
        return document.select("body > section.productCardContent > div > div > div > " +
                "div.productCardContentHeader > div.productCardContentRight > div.sectionTitle > h1").text();
    }

    private String getSiteId(String scu) {
        String numbers = getOnlyNumbers(scu);
        return numbers;
    }

    private String getOnlyNumbers(String text) {
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < text.length(); i ++){
            String charr = text.substring(i, i + 1);
            if(charr.matches("\\d")){
                result.append(charr);
            }
        }
        return result.toString();
    }

    @Override
    public List<Product> getAllProducts() {
        List<String> productLinks = getAllProductLinks();
        List<Product> products = new ArrayList<>();
        for(String link : productLinks){
            try{
                products.add(getProduct(link));
            } catch (Exception e){
                System.out.println("The parsing of product link: " + link +
                        " have failed. The page may have been removed.");
            }
        }
        return products;
    }

    public Document getDocument(String link) throws IOException {
        return Jsoup.connect(link).get();
    }
}
