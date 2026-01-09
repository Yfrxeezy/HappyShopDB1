package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerController {
    public CustomerModel cusModel;

    public void doAction(String action) throws SQLException, IOException {
        switch (action) {
            case "Cancel":
                cusModel.cancel();
                break;
            case "Check Out":
                cusModel.checkOut();
                break;
            case "OK & Close":
                cusModel.closeReceipt();
                break;
        }
    }
    public void searchProducts(String keyword) throws SQLException {
        cusModel.searchProducts(keyword);
    }

    public void addProductToTrolley(Product product, int quantity) {
        product.setOrderedQuantity(quantity);
        cusModel.selectProduct(product);
        cusModel.addToTrolley();
    }
    public void removeFromTrolley(Product p) {
        cusModel.removeProduct(p);
    }
    public void updateTrolleyQuantity(Product p, int qty) {
        cusModel.updateQuantity(p, qty);
    }
}
