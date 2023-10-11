import parsers.KlParser;
import product.Product;
import vkMarketCrud.VkMarketCRUD;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        new VkMarketCRUD("kr", false).update();
        new VkMarketCRUD("kl", false).update();
        new VkMarketCRUD("d", false).update();
    }
}
