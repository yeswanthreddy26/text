package com.mymart.model;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;

public class ProductDto {

	@NotEmpty(message = "The name is  required")
	 private String name;
	 
	 @NotEmpty(message = "The name is required")
	 private String brand;
	 

	 private Category category;
	 
	 @Min(0)
	 private double price;
	 
	 @Size(min = 10, message = "The description should be atleast 10 characters")
	 @Size(max = 2000, message = "The description cannot exceed 2000 characters")
	 private String description;
	 
	 private MultipartFile imageFile;
	 
	 private int ratingCount = 0;
	 

	public int getRatingCount() {
		return ratingCount;
	}

	public void setRatingCount(int ratingCount) {
		this.ratingCount = ratingCount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public MultipartFile getImageFile() {
		return imageFile;
	}

	public void setImageFile(MultipartFile imageFile) {
		this.imageFile = imageFile;
	}
	 
	 
	}
