package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * The CustomerView is separated into two sections by a line :
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 *    depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView  {
    public CustomerController cusController;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    public TextField tfId; //for user input on the search page. Made accessible so it can be accessed or modified by CustomerModel
    TextField tfName; //for user input on the search page. Made accessible so it can be accessed by CustomerModel

    private VBox vbSearchResults; //creates a virtualbox for search results
    private Label lbResultCount; //for it to grab the search result of the item

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private VBox vbTrolleyItems; //in trolley Page
    private Label lbTotalPrice;
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);

        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyle);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Customer Client");
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow=window;// Sets viewWindow to this window for future reference and management.
    }

    private VBox createSearchPage() {
        Label laPageTitle = new Label("Search by Product ID/Name");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        tfId = new TextField();
        tfId.setPromptText("Search Product or ID");
        tfId.setStyle(UIStyle.textFiledStyle);

        tfId.textProperty().addListener((obs, o, n) -> {
            try {
                cusController.searchProducts(n);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        lbResultCount = new Label("0 products Found");
        lbResultCount.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        vbSearchResults = new VBox(5);
        vbSearchResults.setStyle("""
        -fx-background-color: white;
        -fx-border-color: #ccc;
        -fx-padding-color: 5;
        """);

        ScrollPane spResults = new ScrollPane(vbSearchResults);
        spResults.setFitToWidth(true);
        spResults.setPrefHeight(220);
        spResults.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60);
        ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true); // Image keeps its original shape and fits inside 60×60
        ivProduct.setSmooth(true); //make it smooth and nice-looking

        lbProductInfo = new Label("Thank you for shopping with us.");
        lbProductInfo.setWrapText(true);
        lbProductInfo.setStyle(UIStyle.labelMulLineStyle);

        HBox hbPreview = new HBox(10, ivProduct, lbProductInfo);
        hbPreview.setAlignment(Pos.CENTER_LEFT);

        VBox vbSearchPage = new VBox(
                10,
                laPageTitle,
                tfId,
                lbResultCount,
                spResults,
                hbPreview
        );
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }

    //Product Rows
    private HBox createProductRow(Product p) {

        String imgPath = StorageLocation.imageFolder + p.getProductImageName();
        Image img = new Image(
                new java.io.File(imgPath).toURI().toString(),
                40,40, true, true
        );
        ImageView iv = new ImageView(img);
        iv.setFitWidth(40);
        iv.setFitHeight(40);
        iv.setPreserveRatio(true);

        Label lbInfo = new Label(
                p.getProductDescription() +
                        "\n£" + String.format("%.2f", p.getUnitPrice())
        );
        lbInfo.setStyle("-fx-font-style: 12px");

        ComboBox<Integer> cbQty = new ComboBox<>();
        for (int i = 1; i <= Math.min(10, p.getStockQuantity()); i++) {
            cbQty.getItems().add(i);
        }
        cbQty.setValue(1);
        cbQty.setPrefWidth(55);

        Button btnAdd = new Button("+");
        btnAdd.setStyle("""
        -fx-background-color: #4CAF50;
        -fx-text-fill: white;
        -fx-font-weight: bold;""");

        btnAdd.setOnAction(e ->
                cusController.addProductToTrolley(p, cbQty.getValue())
        );

        HBox row = new HBox(10, iv, lbInfo, cbQty, btnAdd);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("""
       -fx-padding: 5;
       -fx-border-color: #eee;
       -fx-border-width: 0 0 1 0;
       """);

        return row;
    }

    public void updateSearchResults(List<Product> products) {
        vbSearchResults.getChildren().clear();
        lbResultCount.setText(products.size() + "products found");

        for (Product p : products) {
            vbSearchResults.getChildren().add(createProductRow(p));
        }
    }

    private VBox CreateTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        vbTrolleyItems = new VBox(5);

        ScrollPane sp = new ScrollPane(vbTrolleyItems);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);

        lbTotalPrice = new Label("Total Price: £0.00");
        lbTotalPrice.setStyle("-fx-font-weight: bold;");

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel, btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        return new VBox(10, laPageTitle, sp, lbTotalPrice, hbBtns);
    }

    private HBox createTrolleyRow(Product p) {
        Label name = new Label(
                p.getProductId() + " " + p.getProductDescription() +" £" + String.format("%.2f",p.getUnitPrice())
        );
        Spinner<Integer> qtySpinner = new Spinner<>(
                1,
                p.getStockQuantity(),
                p.getOrderedQuantity()
        );
        qtySpinner.setPrefWidth(70);
        qtySpinner.setEditable(true);

        qtySpinner.valueProperty().addListener((obs, oldVal, newVal) ->
                cusController.updateTrolleyQuantity(p, newVal)
        );

        Button del = new Button("🗑️");
        del.setOnAction(e -> cusController.removeFromTrolley(p));

        Label total = new Label(
                "£" + String.format("%.2f",
                        p.getUnitPrice() * p.getOrderedQuantity())
        );

        HBox row = new HBox(10, name, qtySpinner, del, total);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-width: 0 0 1 0; -fx-border-color: #eee;");

        return row;
    }

    public void updateTrolley(List<Product> trolley) {
        vbTrolleyItems.getChildren().clear();
        double sum = 0;

        for (Product p : trolley) {
            vbTrolleyItems.getChildren().add(createTrolleyRow(p));
            sum += p.getUnitPrice() * p.getOrderedQuantity();
        }
        lbTotalPrice.setText(
                "Total Price: £" + String.format("%.2f", sum)
        );
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);

        Button close = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        close.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, close);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }


    private void buttonClicked(ActionEvent event) {
        try{
            Button btn = (Button)event.getSource();
            String action = btn.getText();

            if(action.equals("OK & Close")){
                showTrolleyOrReceiptPage(vbTrolleyPage);
            }
            cusController.doAction(action);
        }
        catch(SQLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void update(String imageName, String searchResult, String trolley, String receipt) {

        ivProduct.setImage(new Image(imageName));
        lbProductInfo.setText(searchResult);

        if (!receipt.equals("")) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        } else {
            showTrolleyOrReceiptPage(vbTrolleyPage);
        }
    }

    // Replaces the last child of hbRoot with the specified page.
    // the last child is either vbTrolleyPage or vbReceiptPage.
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                  viewWindow.getWidth(), viewWindow.getHeight());
    }
}
