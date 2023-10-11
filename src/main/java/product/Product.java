package product;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class Product implements Serializable{
    private String VKId;
    private String siteId;
    private String link;
    private String name;
    private String description;
    private int categoryId;
    private String mainImgUrl;
    private List<String> imgUrls;
    private double price;
    private double oldPrice;
    private String scu;
    private int albumId;
    private String videoUrl;

    private static final long serialVersionUID = 1L;

    public String getVKId() {
        return VKId;
    }

    public void setVKId(String VKId) {
        this.VKId = VKId;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getMainImgUrl() {
        return mainImgUrl;
    }

    public void setMainImgUrl(String mainImgUrl) {
        this.mainImgUrl = mainImgUrl;
    }

    public List<String> getImgUrls() {
        return imgUrls;
    }

    public void setImgUrls(List<String> imgUrls) {
        this.imgUrls = imgUrls;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(double oldPrice) {
        this.oldPrice = oldPrice;
    }

    public String getScu() {
        return scu;
    }

    public void setScu(String scu) {
        this.scu = scu;
    }

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public String getVideoUrl(){
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    @Override
    public boolean equals(Object obj) {
//        return false;
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Product product)) {
            return false;
        }

        try{
            if(!product.getVideoUrl().equals(this.getVideoUrl())){
                return false;
            }
        } catch (NullPointerException e){
            return false;
        }

        return product.getLink().equals(this.link) &&
                product.getSiteId().equals(this.siteId) &&
                product.getName().equals(this.name) &&
                product.getDescription().equals(this.description) &&
                product.getPrice() == this.price &&
                product.getMainImgUrl().equals(this.mainImgUrl) &&
                product.getImgUrls().equals(this.imgUrls)&&
                product.getVideoUrl().equals(this.videoUrl);
    }
}
