package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public RemoveProductNotifier proNotifier;
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.

    private Product theProduct =null; // product found from search
    private ArrayList<Product> trolley =  new ArrayList<>(); // a list of products in trolley

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)

    //SELECT productID, description, image, unitPrice,inStock quantity
    public void searchProducts(String Keyword) throws SQLException {
        if (Keyword == null || Keyword.isBlank()) {
            cusView.updateSearchResults(List.of());
            return;
        }
        List<Product> results = databaseRW.searchProduct(Keyword);
        cusView.updateSearchResults(results);
    }

    public void selectProduct(Product product) {
        this.theProduct = product;
        displayLaSearchResult = product.getProductDescription()
                + "\n£" + String.format("%.2f", product.getUnitPrice());
        updateView();
    }

    void search() throws SQLException {
        String productId = cusView.tfId.getText().trim();
        if(!productId.isEmpty()){
            theProduct = databaseRW.searchProduct(productId).getFirst(); //search database

            if(theProduct != null && theProduct.getStockQuantity()>0){
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            }
            else{
                theProduct=null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            }
        }else{
            theProduct=null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");
        }
        updateView();
    }

    void addToTrolley(){
        if(theProduct!= null){
            if(theProduct.getOrderedQuantity() == 0) {
                theProduct.setOrderedQuantity(1);
            }

            // trolley.add(theProduct) — Product is appended to the end of the trolley.
            // To keep the trolley organized, add code here or call a method that:
            //TODO
            // 1. Merges items with the same product ID (combining their quantities).
            // 2. Sorts the products in the trolley by product ID.
            //adding in the organized trolley
            makeOrganizedTrolley();
            cusView.updateTrolley(trolley);
        }
        else{
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }
        displayTaReceipt=""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }

    void makeOrganizedTrolley(){
        for(Product p: trolley){
            if(p.getProductId().equals(theProduct.getProductId())){
               p.setOrderedQuantity(p.getOrderedQuantity()+ theProduct.getOrderedQuantity());
               return;
            }
        }
        trolley.add(theProduct);
        trolley.sort(Comparator.comparing(Product::getProductId));
    }

    public void removeProduct(Product p) {
        trolley.removeIf(t -> t.getProductId().equals(p.getProductId()));
        cusView.updateTrolley(trolley);
    }

    public void updateQuantity(Product p, int qty) {
        for(Product t : trolley) {
            if(t.getProductId().equals(p.getProductId())) {
                t.setOrderedQuantity(qty);
            }
        }
        cusView.updateTrolley(trolley);
    }

    void checkOut() throws IOException, SQLException {
        System.out.println("Checkout Done");
        if(!trolley.isEmpty()){
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> trolleyList = groupProductsById(trolley);
            ArrayList<Product> insufficientProducts= databaseRW.purchaseStocks(trolleyList);

            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
                trolley.clear();
                cusView.updateTrolley(trolley);
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
            }
            else{ // Some products have insufficient stock — build an error message to inform the customer
                displayLaSearchResult = "Checkout Failed Due To Insufficient Stock";
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    //Removing the product with insufficient stock from the trolley list
                    trolleyList.remove(p);
                    //Producing the error format for the error message
                    errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                proNotifier.showRemovalMsg(errorMsg.toString());
                System.out.println("stock is not enough");
            }
        }
        updateView();
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original
                grouped.put(id,new Product(p.getProductId(),p.getProductDescription(),
                        p.getProductImageName(),p.getUnitPrice(),p.getStockQuantity()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        cusView.updateTrolley(trolley);
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
        updateView();
    }

    void updateView() {
        if(theProduct != null){
           Path p = Paths.get(StorageLocation.imageFolder + theProduct.getProductImageName()).toAbsolutePath();
           imageName = p.toUri().toString();
        }
        else{
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult,"",displayTaReceipt);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }
}
