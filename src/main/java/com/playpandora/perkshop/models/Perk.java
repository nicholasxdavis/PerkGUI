package com.playpandora.perkshop.models;

import java.util.List;

public class Perk {
    
    private final String key;
    private final String name;
    private final List<String> description;
    private final double price;
    private final String material;
    private final String permission;
    private final String requires;
    private final Double multiplier;
    private final int requiredLevel;
    
    public Perk(String key, String name, List<String> description, double price, 
                String material, String permission, String requires, Double multiplier, int requiredLevel) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.price = price;
        this.material = material;
        this.permission = permission;
        this.requires = requires;
        this.multiplier = multiplier;
        this.requiredLevel = requiredLevel;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getDescription() {
        return description;
    }
    
    public double getPrice() {
        return price;
    }
    
    public String getMaterial() {
        return material;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public String getRequires() {
        return requires;
    }
    
    public Double getMultiplier() {
        return multiplier;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
}

