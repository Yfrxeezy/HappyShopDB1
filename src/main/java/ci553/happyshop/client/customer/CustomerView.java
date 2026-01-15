package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
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

    ListView<Product> obrLvProducts;
    ListView<Product>obrLvTrolley;

    private VBox vbSearchResults; //creates a virtualbox for search results
    private Label lbResultCount; //for it to grab the search result of the item

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private VBox vbTrolleyItems; //in trolley Page
    private Label lbTotalPrice;
    private ObservableList<Product> obeProductList;
    private ObservableList<Product> obeTrolleyList;
    private TextArea taReceipt;//in receipt page
    private HBox hbSearch;

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = createTrolleyPage();
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

        Button btnSearch = new Button ("\uD83D\uDD0D");
        btnSearch.setPrefWidth(30);
        btnSearch.setStyle("""
        -fx-background-color: #D3D3D3;
        -fx-text-fill: black;
        -fx-font-weight: bold;
        """);
        btnSearch.setOnAction(actionEvent -> {
                    try {
                        cusController.searchProducts(tfId.getText().trim());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
        });
        btnSearch.getProperties().put("search", null);

        HBox hbSearch = new HBox(5,tfId, btnSearch);
        hbSearch.setAlignment(Pos.CENTER);

        lbResultCount = new Label("0 products Found");
        lbResultCount.setStyle("-fx-text-fill: red; -fx-font-size: 12px; -fx-font-weight: bold;");
        lbResultCount.setStyle(UIStyle.labelStyle);

        vbSearchResults = new VBox(5);
        vbSearchResults.setStyle("""
        -fx-background-color: white;
        -fx-border-color: light gray;
        -fx-padding-color: 5;
        """);

        obeProductList = FXCollections.observableArrayList();

        //Listview of displaying products with appealing UI
        obrLvProducts = new ListView<>(obeProductList); //updates the list from the observer
        obrLvProducts.setPrefHeight(HEIGHT - 100);
        obrLvProducts.setFixedCellSize(50);
        obrLvProducts.setStyle(UIStyle.listViewStyle);

        obrLvProducts.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null) {
                    setGraphic(null);
                } else {
                    HBox hbProItem = createProductItem(product);
                    hbProItem.setAlignment(Pos.CENTER);
                    setGraphic(hbProItem);
                }
            }
        });

        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60);
        ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true); // Image keeps its original shape and fits inside 60×60
        ivProduct.setSmooth(true); //make it smooth and nice-looking

        lbProductInfo = new Label("Thank you for shopping with us.");
        lbProductInfo.setWrapText(true);
        lbProductInfo.setStyle(UIStyle.labelMulLineStyle);

        HBox previewBox = new HBox(10, ivProduct, lbProductInfo);
        previewBox.setAlignment(Pos.CENTER_LEFT);

        VBox vbSearchPage = new VBox(10, laPageTitle, hbSearch, lbResultCount,obrLvProducts,previewBox);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }

    private VBox createTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        vbTrolleyItems = new VBox(5);

        ScrollPane sp = new ScrollPane(vbTrolleyItems);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);

        lbTotalPrice = new Label("Total Price: £0.00");
        lbTotalPrice.setStyle("-fx-font-weight: bold;");

        obeTrolleyList = FXCollections.observableArrayList();

        obrLvTrolley = new ListView<>(obeProductList); //updates the list from the observer
        obrLvTrolley.setPrefHeight(HEIGHT - 100);
        obrLvTrolley.setFixedCellSize(50);
        obrLvTrolley.setStyle(UIStyle.listViewStyle);

        obrLvProducts.setCellFactory(param -> new ListCell<Product>() {
                    @Override
                    protected void updateItem(Product product, boolean empty) {
                        super.updateItem(product, empty);

                        if (empty || product == null) {
                            setGraphic(null);
                        } else {
                            HBox hbProItem = createTrolleyItem(product);
                            hbProItem.setAlignment(Pos.CENTER);
                            setGraphic(hbProItem);
                        }
                    }
                });

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel, btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        VBox trolleyBox = new VBox(10, laPageTitle, sp, lbTotalPrice, hbBtns);
        trolleyBox.setAlignment(Pos.TOP_CENTER);
        trolleyBox.setPrefWidth(COLUMN_WIDTH);

        return trolleyBox;
    }

    private HBox createProductItem(Product product) {
        String imageName = product.getProductImageName(); // Get image name (e.g. "0001.jpg")
        String relativeImageUrl = StorageLocation.imageFolder + imageName;
        Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
        String imageFullUri = imageFullPath.toUri().toString();// Build the full image Uri
        ImageView ivPro;
        try {
            ivPro = new ImageView(new Image(imageFullUri, 50, 45, true, true)); // Attempt to load the product image
        } catch (Exception e) {
            // If loading fails, use a default image directly from the resources folder
            ivPro = new ImageView(new Image("imageHolder.jpg", 50, 45, true, true)); // Directly load from resources
        }
        Label laStock = new Label("Pending Stock");

        int stockQuantity = product.getStockQuantity() - product.getOrderedQuantity();
        if (stockQuantity <= 30 && stockQuantity >= 1) {
            //Low Stock Quantity
            laStock.setStyle(UIStyle.labelLowStockStyle);
            laStock.setText("⚠ Low Stock" + stockQuantity);
        } else if (stockQuantity <= 0) {
            laStock.setStyle(UIStyle.labelLOutOfStockStyle);
            laStock.setText("❌ Out Of Stock");
        } else {
            laStock.setStyle(UIStyle.labelInStockStyle);
            laStock.setText("✅ In Stock");
        }
        Label lDetail = new Label(product.getProductDescription());
        Label lId = new Label(product.getProductId());
        Label lPrice = new Label("£" + product.getUnitPrice());
        lPrice.setStyle(UIStyle.labelPriceStyle);
        lId.setStyle(UIStyle.labelIdStyle);

        ComboBox<Integer> comboQ = new ComboBox<>();
        comboQ.setPrefSize(65, 33);
        for (int i = 1; i <= 10; i++) {
            if (i == 1) {
                comboQ.setValue(i);
            }
            comboQ.getItems().add(i);
        }

        Button btnAdd = new Button("\uD83D\uDED2");
        btnAdd.setPrefSize(35, 33);
        btnAdd.setOnAction(this::buttonClicked);
        btnAdd.getProperties().put("Add To Trolley", Arrays.asList(product, comboQ.getValue()));

        comboQ.valueProperty().addListener((obs, oldVal, newVal) -> {
        btnAdd.getProperties().put("Add To Trolley", Arrays.asList(product, comboQ.getValue()));
        });
        HBox HBMiddle = new HBox(5,lPrice, lId);
        HBMiddle.setAlignment(Pos.CENTER_LEFT);
        VBox vbTop = new VBox(0, lDetail, HBMiddle, laStock);
        vbTop.setAlignment(Pos.CENTER_LEFT);
        HBox hbRight = new HBox(5, comboQ, btnAdd);
        hbRight.setAlignment(Pos.CENTER);
        return new HBox(10, ivPro, vbTop, hbRight);
    }

    private HBox createTrolleyItem(Product p) {
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

        Button btnDel = new Button("\uD83D\uDDD1\uFE0F");
        btnDel.setOnAction(e -> cusController.removeFromTrolley(p));
        btnDel.setStyle("""
        -fx-background-color: #D3D3D3;
        -fx-text-fill: red;
        """);

        Label total = new Label(
                "£" + String.format("%.2f",
                        p.getUnitPrice() * p.getOrderedQuantity())
        );

        HBox row = new HBox(10, name, qtySpinner, btnDel, total);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-width: 0 0 1 0; -fx-border-color: white gray;");

        return row;
    }

    public void updateTrolley(List<Product> trolley) {
        vbTrolleyItems.getChildren().clear();
        double sum = 0;

        for (Product p : trolley) {
            vbTrolleyItems.getChildren().add(createTrolleyItem(p));
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
            String action = (String) btn.getUserData();

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

    public void updateSearchResults(List<Product> products) {
        obeProductList.clear();

        if(products == null || products.isEmpty()) {
            lbResultCount.setText("0 products found");
            return;
        }
        obeProductList.addAll(products);
        lbResultCount.setText(products.size() + "products found");
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
