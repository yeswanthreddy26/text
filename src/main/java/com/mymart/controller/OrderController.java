package com.mymart.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mymart.model.Address;
import com.mymart.model.CartItem;
import com.mymart.model.DropdownItem;
import com.mymart.model.NavLink;
import com.mymart.model.OrderItem;
import com.mymart.model.OrderStatus;
import com.mymart.model.Orders;
import com.mymart.model.Product;
import com.mymart.model.ShippingMethod;
import com.mymart.model.User;
import com.mymart.repository.AddressRepository;
import com.mymart.repository.ProductsRepository;
import com.mymart.service.AddressService;
import com.mymart.service.CartItemService;
import com.mymart.service.EmailService;
import com.mymart.service.NavBarService;
import com.mymart.service.OrderItemService;
import com.mymart.service.OrderService;
import com.mymart.service.ProductService;
import com.mymart.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
@Controller
 public class OrderController {
	@Autowired
    private UserService userService;

	@Autowired
    private CartItemService cartService;
	@Autowired
	OrderItemService orderItemService;
    @Autowired
    private AddressService addressService;
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductsRepository productsrepo;
    @Autowired
    private NavBarService service; 
    @Autowired
    private JavaMailSender emailSender;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ProductService productService;
    @Autowired
    private HttpServletRequest request;

    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, Principal principal) {
    	Map<NavLink, List<DropdownItem>> navbarWithDropdownData = service.getNavbarWithDropdownData();
    	model.addAttribute("navbarWithDropdownData", navbarWithDropdownData);
        User user = userService.getCurrentUser();
        List<Address> addresses = user.getAddresses(); // Fetch user's addresses
        List<Product> products = productService.getAllProducts(); // Fetch available products
        List<CartItem> cartItems = cartService.getCartItems(user); // Fetch cart items for the user

        if (addresses.isEmpty()) {
            return "redirect:/addAddress"; // Redirect to add address page if no addresses are available
        }

        Address defaultAddress = addresses.get(0);
        


        double subtotal = cartService.calculateSubtotal(cartItems);
        double shipping = cartService.calculateShipping(subtotal);
        double total = subtotal + shipping;


        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("products", products);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("shipping", shipping);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("total", total);
        model.addAttribute("defaultAddress", defaultAddress);

        return "checkout";
    }


    @GetMapping("/addAddress")
    public String showAddAddressPage(Model model) {
        model.addAttribute("address", new Address());
        return "addAddress";
    }
    @PostMapping("/checkout/addAddress")
    public String addAddress(@ModelAttribute("address") Address address, Principal principal) {
        User user = userService.getCurrentUser();
        address.setUser(user);
        addressService.saveAddress(address);
        return "redirect:/checkout"; // Redirect back to checkout page
    }
  
    
    @GetMapping("/editAddress/{id}")
    public String showEditAddressPage(@PathVariable("id") int id, Model model) {
        Address address = addressService.getAddressById(id);
        model.addAttribute("address", address);
        model.addAttribute("id", id);
        return "editAddress";
    }
    


  
    @PostMapping("/editAddress/{id}")
    public String editAddress(@PathVariable("id") int id, @ModelAttribute("address") Address address, Principal principal) {
        // Fetch the logged-in user's ID
    	 User user = userService.getCurrentUser();
         address.setUser(user);

        address.setId(id);
        addressService.saveAddress(address);
        return "redirect:/checkout"; // Redirect back to the edited address page
    }

    @GetMapping("/deleteAddress/{id}")
    public String deleteAddress(@PathVariable("id") int id,Principal principal) {
    	 User user = userService.getCurrentUser();
        Address address = addressService.getAddressById(id);

        addressRepository.delete(address);
        return "redirect:/checkout";
    }
    
    
  

    
    @GetMapping("/orderConfirmation")
    public String orderConfirmation(@RequestParam("addressId") Integer addressId, Model model, Principal principal) {
         String username = principal.getName();
         User user1 = userService.getCurrentUser();
         List<Address> addresses = user1.getAddresses(); // Fetch user's addresses
         User user = userService.findByEmail(username);

        List<CartItem> cartItems = cartService.getCartItems(user); // Fetch cart items for the user

        Orders orders = orderService.getLatestOrderForUser(user); // Fetch the most recent order for the user

        Address defaultAddress = addresses.get(0);

        double subtotal = cartService.calculateSubtotal(cartItems);
        double shipping = cartService.calculateShipping(subtotal);
        double total = subtotal + shipping;

        model.addAttribute("user", user);

        model.addAttribute("addresses", addresses);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("orders", orders);
        model.addAttribute("shipping", shipping);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("total", total);
        model.addAttribute("defaultAddress", defaultAddress);

        Optional<Address> optionalAddress = addressRepository.findById(addressId);
        if (optionalAddress.isPresent()) {
            model.addAttribute("selectedAddress", optionalAddress.get());
            return "orderConfirm";
        } else {
            return "redirect:/error";
        }
    }
    
    @PostMapping("/placeOrder/COD")
    public String placeOrder(@RequestParam("addressId") int addressId, Orders orders, Principal principal,Model model) {
        try {
            if (principal == null) {
                return "redirect:/login"; // Redirect to login if user is not authenticated
            }

            String username = principal.getName(); // Get the username from Principal
            User user = userService.findByEmail(username);
            if (user == null) {
                return "redirect:/login"; // Redirect to login if user not found
            }

            orders.setUser(user); // Set the user in the order object

            Address shippingAddress = addressService.findById(addressId); // Fetch the Address object by ID
            if (shippingAddress == null) {
                return "Invalid address ID";
            }

            orders.setShippingAddresses(shippingAddress); // Set the shipping address in the Orders object
 
	        List<CartItem> cartItems = cartService.getAllCartItemsByUser(user);
            double subtotal = cartService.calculateSubtotal(cartItems);
            
            
            if (subtotal == 0) {
                model.addAttribute("error", "Product not added. Please add products to Place the order.");
                return "redirect:/cart"; // Redirect to cart with error message
            }
            
            
            
	        double shipping = cartService.calculateShipping(subtotal);
	        
	        double total = subtotal + shipping;

	        
	        model.addAttribute("subtotal", subtotal);
	        model.addAttribute("shipping", shipping);
	        model.addAttribute("total", total);
            
            orders.setSubtotal(subtotal);

          orders.setShippingCharges(shipping); // Set the shipping charges in the Orders object

         
          orders.setTotalAmount(total);
         orders.setAmount(String.valueOf(total));
          String orderNumber = generateOrderNumber();

          if (orders.getShippingMethod() == null) {
              orders.setShippingMethod(ShippingMethod.STANDARD);
          }
          orders.setStatus(OrderStatus.PLACED);
          
          orders.setOrderNumber(orderNumber);
           orders.setPaymentTransactionId("COD");
          
          Optional<Address> optionalAddress = addressRepository.findById(addressId);
	       
	            model.addAttribute("selectedAddress", optionalAddress.get());
	            
          
            orderService.saveOrder(orders); // Save the order
            
            // Send order confirmation email
            sendOrderConfirmationEmail(user.getEmail(), orders);

            
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(orders); // Set the order for the order item
                orderItem.setProduct(cartItem.getProduct()); // Set the product for the order item
                orderItem.setQuantity(cartItem.getQuantity()); // Set the quantity for the order item

                // Calculate and set the total price
                double totalPrice = cartItem.getQuantity() * cartItem.getProduct().getPrice();
                orderItem.setTotalPrice(totalPrice);

                // Save the order item
                orderItemService.saveOrderItem(orderItem);
            }

            
            cartService.clearCart(user);
            emailService.sendOrderConfirmationEmail(user.getEmail(), orders);
           return "redirect:/orderConfirmation?addressId=" + addressId;
              
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception for debugging
            return "Error storing form data";
        }
    }

    private String generateOrderNumber() {
        return "COD" + System.currentTimeMillis();
    } 
    
   

    @GetMapping("/orders")
    public String showUserOrders(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        User user = userService.findByEmail(username);
        List<Orders> userOrders = orderService.getOrdersByUser(user);

        model.addAttribute("userOrders", userOrders);
        return "user_orders"; // Assuming you have a Thymeleaf template for displaying user orders
    }
    
    
    @GetMapping("/admin/orders")
    public String showAdminOrders(Model model) {
        List<Orders> order = orderService.getAllOrders();
        model.addAttribute("orders", order);
        return "admin/orders";
    }

    @PostMapping("/admin/orders/{orderId}/accept")
    public String acceptOrder(@PathVariable("orderId") int orderId) {
        Orders order = orderService.getOrderById(orderId);
        if (order == null) {
            // Handle error when order is not found
            return "redirect:/admin/orders?error";
        }
        order.setStatus(OrderStatus.ACCEPTED); // Update order status to "Accepted"
        orderService.updateOrder(order);
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{orderId}/cancel")
    public String cancelOrder(@PathVariable("orderId") int orderId) {
        Orders order = orderService.getOrderById(orderId);
        if (order == null) {
            // Handle error when order is not found
            return "redirect:/admin/orders?error";
        }
        order.setStatus(OrderStatus.CANCELLED); // Update order status to "Canceled"
        orderService.updateOrder(order);
        
        sendCancellationEmailToUser(order.getUser().getEmail(), order);
        
        return "redirect:/admin/orders";
    }
    
    @PostMapping("/admin/orders/{orderId}/mark-delivered")
    public String markOrderAsDelivered(@PathVariable("orderId") int orderId) {
        Orders order = orderService.getOrderById(orderId);
        if (order == null) {
            // Handle error when order is not found
            return "redirect:/admin/orders?error";
        }
        order.setStatus(OrderStatus.DELIVERED);
        orderService.updateOrder(order);
        
        sendDeliveryConfirmationEmailToUser(order.getUser().getEmail(), order);
        
        return "redirect:/admin/orders";
    }
    
    public void sendOrderConfirmationEmail(String userEmail, Orders orders) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("MyMart Order Confirmation - Order #" + orders.getOrderNumber());

        // Build the message body with the tracking link
        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Thank you for your order!\n\nYour order has been successfully placed. Order details:\n\n");
        emailBody.append("Order Number: ").append(orders.getOrderNumber()).append("\n\n");
        
        // Construct the full tracking link with the domain and port
        String trackingLink = "For More Details And track your order, please follow this link: http://localhost:8080/trackOrder/" + orders.getId() + "\"";
        emailBody.append(trackingLink).append("\n\n");

        emailBody.append("Total Amount: $").append(orders.getTotalAmount()).append("\n\n");
        emailBody.append("Thank you for shopping with us!\n\nMyMart Team");

        message.setText(emailBody.toString());
        emailSender.send(message);
    }

    
    private void sendDeliveryConfirmationEmailToUser(String userEmail, Orders orders) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("MyMart Order Delivered - Order #" + orders.getOrderNumber());
        message.setText("Dear Customer,\n\nWe are pleased to inform you that your order with Order Number "
                + orders.getOrderNumber() + " has been delivered.\n\nThank you for shopping with us.\n\nMyMart Team");
        emailSender.send(message);
    }
    
    private void sendCancellationEmailToUser(String userEmail, Orders orders) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("MyMart Order Cancellation - Order #" + orders.getOrderNumber());
        message.setText("Dear Customer,\n\nWe regret to inform you that your order with Order Number "
                + orders.getOrderNumber() + " has been cancelled.\n\nIf you have any questions or concerns, please feel free to contact us.\n\nThank you.\n\nMyMart Team");
        emailSender.send(message);
    }
    
    @GetMapping("/total")
    public ResponseEntity<Long>GetTotalOrder()
    {
        long totalorders=orderService.GetTotalOrder();
        return ResponseEntity.ok(totalorders);

    }

    
    @GetMapping("/totalsales")
    public ResponseEntity<Double> getTotalSales() {
        Double totalSales = orderItemService.getTotalSales();
        return ResponseEntity.ok(totalSales);
    }
    //Top selling Products.
    @GetMapping("/topsellingproducts")
    public ResponseEntity<List<Object[]>> getTopSellingProducts() {
        List<Object[]> topSellingProducts = orderItemService.getTopSellingProducts();
        return ResponseEntity.ok(topSellingProducts);
    }
    
    @GetMapping("/buynow")
    public String showbuynowpage(
             Model model, Principal principal,@RequestParam int id
    ) {

        try {
            User user = userService.getCurrentUser();
            List<Address> addresses = user.getAddresses(); // Fetch user's addresses
            List<Product> products = productService.getAllProducts(); // Fetch available products
            List<CartItem> cartItems = cartService.getCartItems(user); // Fetch cart items for the user
            Product product1=new Product();
            if (addresses.isEmpty()) {
                return "redirect:/addAddress"; // Redirect to add address page if no addresses are available
            }

            Address defaultAddress = addresses.get(0);



            double subtotal = cartService.calculateSubtotal(cartItems);
            double shipping = cartService.calculateShipping(subtotal);



            model.addAttribute("user", user);
            model.addAttribute("addresses", addresses);
            model.addAttribute("products", products);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("shipping", shipping);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("defaultAddress", defaultAddress);

            Product product=productsrepo.findById(id).get();
            model.addAttribute("product", product);
            double finalprice = product.getPrice() + shipping;
            model.addAttribute("finalprice", finalprice);


            return "buynow";

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception is : "+e.getMessage());
            return "redirect:logout";
        }



}
}