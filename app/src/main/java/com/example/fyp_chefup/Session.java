package com.example.fyp_chefup;

public class Session {
    private String foodName;
    private String equipment;
    private String ingredients;
    private String contactInfo;
    private String address;
    private String chefId;
    private String imageUrl;

    // Update the constructor to initialize chefId
    public Session(String foodName, String equipment, String ingredients, String contactInfo, String address, String chefId, String imageUrl) {
        this.foodName = foodName;
        this.equipment = equipment;
        this.ingredients = ingredients;
        this.contactInfo = contactInfo;
        this.address = address;
        this.chefId = chefId;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getChefId() { return chefId; }
    public void setChefId(String chefId) { this.chefId = chefId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl; }
}
