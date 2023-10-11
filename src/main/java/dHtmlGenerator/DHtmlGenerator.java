package dHtmlGenerator;

import parsers.DParser;
import product.Product;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DHtmlGenerator {
    private final DParser dparser = new DParser();

    public void printAllCorrectPricesHtml(){
        for(Product product : dparser.getAllProducts()){
            System.out.println(product.getLink());
            String name = product.getName().trim();
            String address = name.split("є\\s*\\d*")[1].trim();
            String rooms = name.substring(0, 1);
            System.out.println(getPricesHtml(getApartmentPricesList(address, rooms)));
        }
    }

    public void printAllPricesHtmlToUpdate() {
        for(Product product : dparser.getAllProducts()){
            try{
                System.out.println(product.getLink());
                String name = product.getName().trim();
                String address = name.split("є\\s*\\d*")[1].trim();
                String rooms = name.substring(0, 1);
                List<String> prices = getApartmentPricesList(address, rooms);
                List<Double> sitePrices = getSitePricesList(product);
                if(sitePrices.get(0) == Double.parseDouble(prices.get(2)) &&
                        sitePrices.get(1) == Double.parseDouble(prices.get(3))){
                    System.out.println(product.getLink() + " does not changed.");
                } else {
                    System.out.println(sitePrices.get(0) + "!=" + prices.get(2) +  " || " + sitePrices.get(1) + "!=" + prices.get(3));
                    System.out.println(getPricesHtml(prices));
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public String generateApartmentPageHtml(ApartmentPage page) throws IOException{
        List<Product> products = dparser.getProducts(page);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<center>");
        for(Product product : products){
            stringBuilder.append(getProductHtml(product));
        }
        stringBuilder.append("\n</center>");
        System.out.println("amount of posts: " + products.size());
        return stringBuilder.toString();
    }

    private String getProductHtml(Product product){
        return "\n\n<!--" +
                product.getSiteId() +
                "-->\n<h2 id=\"pagetitle\"><a href=\"" +
                product.getLink() +
                "\">є" +
                product.getName().split("є")[1].trim() +
                " от " +
                (int) product.getPrice() +
                " руб.</a></h2>\n" +
                "<figure class=\"wp-block-image is-resized\"><a href=\"" +
                product.getLink() +
                "\"><img src=\"" +
                product.getMainImgUrl() +
                "\"/></a></figure>";
    }
    private List<Double> getSitePricesList(Product product) throws IOException {
        List<Double> prices = new ArrayList<>();
        prices.add(dparser.getHalfPrice(dparser.getDocument(product.getLink())));
        prices.add(product.getPrice());
        return prices;
    }

    private List<List<String>> getAllPrices() {
        List<List<String>> allPrices = new ArrayList<>();
        try(FileReader fileReader = new FileReader("src/main/resources/dekabrist/prices.txt");
            BufferedReader reader = new BufferedReader(fileReader)){
            while (reader.ready()){
                String prices = reader.readLine();
                List<String> pricesList = new ArrayList<>();
                for (String price : prices.split("/")){
                    pricesList.add(price.trim());
                }
                allPrices.add(pricesList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allPrices;
    }

    public String getPricesHtml(List<String> pricesList) {
        return "\n \t<li>Ч 12 часов (полсуток) Ц<span style=\"color: #e64b3c;\"> от " +
                pricesList.get(2) +
                " </span>руб.;</li>" +
                "\n \t<li>Ч 1-13 суток Ц<span style=\"color: #e64b3c;\"> от " +
                pricesList.get(3) +
                " </span>руб./сутки;</li>" +
                "\n \t<li>Ч 14-20 суток Ц<span style=\"color: #e64b3c;\"> от " +
                pricesList.get(4) +
                " </span>руб./сутки;</li>" +
                "\n \t<li>Ч 21-30 суток Ц<span style=\"color: #e64b3c;\"> от " +
                pricesList.get(5) +
                " </span>руб./сутки;</li>\n" +
                " \t<li>Ч Ѕолее 30 суток Ц <span style=\"color: #e64b3c;\"> индивидуальные услови€</span>;</li>\n" +
                " \t<li>Ч 3 часа - <span style=\"color: #e64b3c;\">1200</span> рублей. ѕродление - <span style=\"color: #e64b3c;\">200</span> рублей/час;</li>\n" +
                " \t<li>Ч ƒополнительное спальное место Ц<span style=\"color: #e64b3c;\"> 390 </span>руб.</li>";
    }

    private List<String> getApartmentPricesList(String address, String rooms) {
        List<String> apartmentPrices = new ArrayList<>();
        for(List<String> fileApartmentPrices : getAllPrices()){
            int fileRooms = getCorrectRoomsNumber(fileApartmentPrices.get(0));
            String fileAddress = getCorrectAddress(fileApartmentPrices.get(1));
            if(fileRooms == getCorrectRoomsNumber(rooms) && fileAddress.equalsIgnoreCase(getCorrectAddress(address))){
                System.out.println(fileRooms + " == " + rooms + " && " + fileAddress + " == " + address);
                if(apartmentPrices.isEmpty()){
                    apartmentPrices = fileApartmentPrices;
                } else{
                    throw new RuntimeException("¬ списке 2 квартиры соответствуют данным параметрам: " +
                            "адрес - " + address + ", количество комнат - " + rooms);
                }
            }
        }
        if(apartmentPrices.isEmpty()){
            throw new RuntimeException("¬ списке нет квартир, соответстующей данным параметрам: " +
                    "адрес - " + address + ", количество комнат - " + rooms);
        }
        return apartmentPrices;
    }

    private String getCorrectAddress(String fileAddress) {
        String correctAddress = fileAddress;
        correctAddress = correctAddress.replace(",", "");
        String[] parts = correctAddress.split(" +");
        String houseNumber = "";
        String street;
        for(int i = parts.length - 1; i > 0; i --){
            String firstChar = Character.toString(parts[i].charAt(0));
            if(firstChar.matches("\\d")){
                houseNumber = parts[i];
                try {
                    houseNumber = houseNumber.split("-")[0];
                } catch (Exception ignored){}
                try {
                    houseNumber = houseNumber.split("к")[0];
                } catch (Exception ignored){}
                StringBuilder houseNumberWithoutLetters = new StringBuilder();
                for(Character ch : houseNumber.toCharArray()){
                    if(Character.toString(ch).matches("\\d")){
                        houseNumberWithoutLetters.append(ch);
                    }
                }
                houseNumber = houseNumberWithoutLetters.toString();
                break;
            }
        }
        if(!houseNumber.isEmpty()){
            street = correctAddress.split(houseNumber)[0].trim();
            street = street.replace("мкр.", "");
            street = street.replace("мкр", "");
            street = street.replace("ул.", "");
            street = street.replace("ул", "");
            correctAddress = street.trim() + " " + houseNumber;
        }
        return correctAddress;
    }

    private int getCorrectRoomsNumber(String roomsString){
        for(Character ch : roomsString.toCharArray()){
            String firstChar = Character.toString(ch);
            if(firstChar.matches("\\d")){
                return Integer.parseInt(firstChar);
            }
        }
        throw new RuntimeException("The number of rooms not defined for string " + roomsString);
    }
}
