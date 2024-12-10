package com.mymart.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mymart.ProductFilter;
import com.mymart.model.Category;
import com.mymart.model.Deal;
import com.mymart.model.Filter;
import com.mymart.model.Product;
import com.mymart.model.ProductDto;
import com.mymart.model.Rating;
import com.mymart.model.User;
import com.mymart.repository.ProductsRepository;
import com.mymart.repository.RatingRepository;
import com.mymart.service.CategoryService;
import com.mymart.service.DealService;
import com.mymart.service.FilterService;
import com.mymart.service.ProductService;
import com.mymart.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/User")
public class UserProductsController {
    
    private final ProductsRepository repo;
    private final DealService dealService;
    private final FilterService filterService;
    private final UserService userService;
    private final RatingRepository ratingRepository;
    private final ProductService productService;
    private final CategoryService categoryService;

    @Autowired
    public UserProductsController(ProductsRepository repo, DealService dealService, FilterService filterService, 
                                  UserService userService, RatingRepository ratingRepository, 
                                  ProductService productService, CategoryService categoryService) {
        this.repo = repo;
        this.dealService = dealService;
        this.filterService = filterService;
        this.userService = userService;
        this.ratingRepository = ratingRepository;
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("{categoryName}")
    public String displayProductsByCategory(@PathVariable String categoryName, Model model) {
        Category category = categoryService.getCategoryByName(categoryName);

        if (category == null) {
            return "error";
        }

        List<Product> products = productService.getProductsByCategory(category);
        List<Deal> deals = dealService.getAllDeals();
        
        Map<Integer, Double> averageRatings = new HashMap<>();
        Map<Integer, Integer> ratingCounts = new HashMap<>();
        
        for (Product product : products) {
            double averageRating = calculateAverageRating(product.getId());
            int ratingCount = countRatingsForProduct(product.getId());
            
            averageRatings.put(product.getId(), averageRating);
            ratingCounts.put(product.getId(), ratingCount);
        }

        model.addAttribute("deals", deals);
        model.addAttribute("category", category);
        model.addAttribute("products", products);
        model.addAttribute("averageRatings", averageRatings);
        model.addAttribute("ratingCounts", ratingCounts);
        
        return "products/UserProduct"; 
    }

    @GetMapping("/All Products")
    public String showProductList(Model model) {       
        List<Product> products = repo.findAll();
        List<Deal> deals = dealService.getAllDeals();
        
        Map<Integer, Double> averageRatings = new HashMap<>();
        Map<Integer, Integer> ratingCounts = new HashMap<>();
        
        for (Product product : products) {
            double averageRating = calculateAverageRating(product.getId());
            int ratingCount = countRatingsForProduct(product.getId());
            
            averageRatings.put(product.getId(), averageRating);
            ratingCounts.put(product.getId(), ratingCount);
        }

        model.addAttribute("products", products);
        model.addAttribute("averageRatings", averageRatings);
        model.addAttribute("ratingCounts", ratingCounts);
        model.addAttribute("deals", deals);
        
        return "products/UserProduct";
    }

    @GetMapping("/viewproduct")
    public String showEditPage1(Model model, @RequestParam int id,
                                @RequestParam(defaultValue = "dateTime") String sortField,
                                @RequestParam(defaultValue = "DESC") String sortDirection,
                                Principal principal) {
        try {
            model.addAttribute("categories", categoryService.getAllCategories());

            Product product = repo.findById(id).orElse(null);
            if (product == null) {
                return "redirect:/Products";
            }

            model.addAttribute("product", product);

            ProductDto productDto = new ProductDto();
            productDto.setName(product.getName());
            productDto.setBrand(product.getBrand());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());
            productDto.setDescription(product.getDescription());
            model.addAttribute("productDto", productDto);

            
//            List<Rating> reviews = productService.getSortedRatings(product, sortField, sortDirection);
//            model.addAttribute("reviews", reviews);
//            model.addAttribute("sortField", sortField);
//            model.addAttribute("sortDirection", sortDirection);

            double averageRating = calculateAverageRating(product.getId());
            model.addAttribute("averageRating", averageRating);

            int ratingCount = countRatingsForProduct(id);
            model.addAttribute("ratingCount", ratingCount);

            if (principal != null) {
                String username = principal.getName(); // Get username from Principal
                User currentUser = userService.findByEmail(username); // Modified to use findByEmail
                Rating userRating = ratingRepository.findByUserAndProduct(currentUser, product);
                model.addAttribute("userRating", userRating != null ? userRating.getRating() : 0);
            }

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            return "redirect:/Products";
        }
        return "products/viewproduct";
    }

    @PostMapping("/rateProduct")
    public String rateProduct(@RequestParam("productId") int productId,
                              @RequestParam("rating") double rating,
                              @RequestParam("review") String review,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {

        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to submit.");
            return "redirect:/viewproduct?id=" + productId;
        }

        User currentUser = userService.getCurrentUser();
        Product product = productService.getProductById(productId);

        if (product == null || currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to submit.");
            return "redirect:/User/viewproduct?id=" + productId;
        }

        Rating existingRating = ratingRepository.findByUserAndProduct(currentUser, product);

        LocalDateTime dateTime = LocalDateTime.now(); // Get current date and time

        if (existingRating != null) {
            existingRating.setRating(rating);
            existingRating.setReview(review);
            existingRating.setDateTime(dateTime); // Set the date and time
            ratingRepository.save(existingRating);
        } else {
            Rating newRating = new Rating();
            newRating.setUser(currentUser);
            newRating.setProduct(product);
            newRating.setRating(rating);
            newRating.setReview(review);
            newRating.setDateTime(dateTime); // Set the date and time
            ratingRepository.save(newRating);
        }

        redirectAttributes.addFlashAttribute("success", "Rating and review submitted successfully.");
        return "redirect:/User/viewproduct?id=" + productId;
    }

    public double calculateAverageRating(int productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return 0.0; 
        }
        List<Rating> ratings = product.getRatings();
        if (ratings.isEmpty()) {
            return 0.0; 
        }
        double sum = 0;
        for (Rating rating : ratings) {
            sum += rating.getRating();
        }
        double averageRating = sum / ratings.size();
        averageRating = Math.round(averageRating * 100.0) / 100.0;
        return averageRating;
    }

    public int countRatingsForProduct(int productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return 0;
        }
        List<Rating> ratings = product.getRatings();
        return ratings.size();
    }

    @PostMapping("/deleteRating")
    public String deleteRating(@RequestParam("productId") int productId,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to delete rating.");
            return "redirect:/User/viewproduct?id=" + productId;
        }
        User currentUser = userService.getCurrentUser();
        Product product = productService.getProductById(productId);
        if (product == null || currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to delete rating.");
            return "redirect:/User/viewproduct?id=" + productId;
        }
        Rating userRating = ratingRepository.findByUserAndProduct(currentUser, product);
        if (userRating != null) {
            ratingRepository.delete(userRating);
            redirectAttributes.addFlashAttribute("success", "Rating deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No rating found to delete.");
        }
        return "redirect:/User/viewproduct?id=" + productId;
    }


	 

	 
}


