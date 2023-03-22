package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderClientEquipmentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class OrderRegionBeautifyModel {
    private String id;

    private String title;
    private Double priceht;
    private Double tva5;
    private Double tva20;
    private Double unitedPriceTTC;
    private Double price;
    private Double totalPriceHT;
    private Double totalPriceTTC;
    private String grade;
    private String name;
    private String image;
    private String ean;
    private String editor;
    private String diffusor;
    private String type;
    private String uaiStructure;
    private String nameStructure;
    private String addressStructure;
    private String eanLDE;
    private String typeCatalogue;

    //not in Json Result
    private Integer totalFree;
    private List<OrderRegionBeautifyModel> offers;

    private OrderRegionComplex orderRegionComplex;

    public String getId() {
        return id;
    }

    public OrderRegionBeautifyModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public OrderRegionBeautifyModel setTitle(String title) {
        this.title = title;
        return this;
    }

    public Double getPriceht() {
        return priceht;
    }

    public OrderRegionBeautifyModel setPriceht(Double priceht) {
        this.priceht = priceht;
        return this;
    }

    public Double getTva5() {
        return tva5;
    }

    public OrderRegionBeautifyModel setTva5(Double tva5) {
        this.tva5 = tva5;
        return this;
    }

    public Double getTva20() {
        return tva20;
    }

    public OrderRegionBeautifyModel setTva20(Double tva20) {
        this.tva20 = tva20;
        return this;
    }

    public Double getUnitedPriceTTC() {
        return unitedPriceTTC;
    }

    public OrderRegionBeautifyModel setUnitedPriceTTC(Double unitedPriceTTC) {
        this.unitedPriceTTC = unitedPriceTTC;
        return this;
    }

    public Double getPrice() {
        return price;
    }

    public OrderRegionBeautifyModel setPrice(Double price) {
        this.price = price;
        return this;
    }

    public Double getTotalPriceHT() {
        return totalPriceHT;
    }

    public OrderRegionBeautifyModel setTotalPriceHT(Double totalPriceHT) {
        this.totalPriceHT = totalPriceHT;
        return this;
    }

    public Double getTotalPriceTTC() {
        return totalPriceTTC;
    }

    public OrderRegionBeautifyModel setTotalPriceTTC(Double totalPriceTTC) {
        this.totalPriceTTC = totalPriceTTC;
        return this;
    }

    public String getGrade() {
        return grade;
    }

    public OrderRegionBeautifyModel setGrade(String grade) {
        this.grade = grade;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrderRegionBeautifyModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getImage() {
        return image;
    }

    public OrderRegionBeautifyModel setImage(String image) {
        this.image = image;
        return this;
    }

    public String getEan() {
        return ean;
    }

    public OrderRegionBeautifyModel setEan(String ean) {
        this.ean = ean;
        return this;
    }

    public String getEditor() {
        return editor;
    }

    public OrderRegionBeautifyModel setEditor(String editor) {
        this.editor = editor;
        return this;
    }

    public String getDiffusor() {
        return diffusor;
    }

    public OrderRegionBeautifyModel setDiffusor(String diffusor) {
        this.diffusor = diffusor;
        return this;
    }

    public String getType() {
        return type;
    }

    public OrderRegionBeautifyModel setType(String type) {
        this.type = type;
        return this;
    }

    public String getUaiStructure() {
        return uaiStructure;
    }

    public OrderRegionBeautifyModel setUaiStructure(String uaiStructure) {
        this.uaiStructure = uaiStructure;
        return this;
    }

    public String getNameStructure() {
        return nameStructure;
    }

    public OrderRegionBeautifyModel setNameStructure(String nameStructure) {
        this.nameStructure = nameStructure;
        return this;
    }

    public String getAddressStructure() {
        return addressStructure;
    }

    public OrderRegionBeautifyModel setAddressStructure(String addressStructure) {
        this.addressStructure = addressStructure;
        return this;
    }

    public String getEanLDE() {
        return eanLDE;
    }

    public OrderRegionBeautifyModel setEanLDE(String eanLDE) {
        this.eanLDE = eanLDE;
        return this;
    }

    public String getTypeCatalogue() {
        return typeCatalogue;
    }

    public OrderRegionBeautifyModel setTypeCatalogue(String typeCatalogue) {
        this.typeCatalogue = typeCatalogue;
        return this;
    }

    public OrderRegionComplex getOrderRegionComplex() {
        return orderRegionComplex;
    }

    public OrderRegionBeautifyModel setOrderRegionComplex(OrderRegionComplex orderRegionComplex) {
        this.orderRegionComplex = orderRegionComplex.clone();
        return this;
    }

    public OrderRegionEquipmentModel getOrderRegion() {
        return this.orderRegionComplex.getOrderRegion();
    }

    public Campaign getCampaign() {
        return this.orderRegionComplex.getCampaign();
    }

    public StudentsTableModel getStudents() {
        return this.getOrderRegionComplex().getStudents();
    }

    public ProjectModel getProject() {
        return this.orderRegionComplex.getProject();
    }

    public Integer getTotalFree() {
        return totalFree;
    }

    public OrderRegionBeautifyModel setTotalFree(Integer totalFree) {
        this.totalFree = totalFree;
        return this;
    }

    public List<OrderRegionBeautifyModel> getOffers() {
        return offers;
    }

    public OrderRegionBeautifyModel setOffers(List<OrderRegionBeautifyModel> offers) {
        this.offers = offers;
        return this;
    }

    public JsonObject toJsonFormat() {
        return new JsonObject().put(Field.ID, this.id)
                .put(Field.AMOUNT, this.getOrderRegion().getAmount())
                .put(Field.CREATION_DATE, this.getOrderRegion().getCreationDate())
                .put(Field.MODIFICATION_DATE, this.getOrderRegion().getModificationDate())
                .put(Field.OWNER_NAME, this.getOrderRegion().getOwnerName())
                .put(Field.OWNER_ID, this.getOrderRegion().getOwnerId())
                .put(Field.STATUS, this.getOrderRegion().getStatus())
                .put(Field.EQUIPMENT_KEY, this.getOrderRegion().getEquipmentKey())
                .put(Field.ID_CAMPAIGN, this.getOrderRegion().getIdCampaign())
                .put(Field.ID_STRUCTURE, this.getOrderRegion().getIdStructure())
                .put(Field.CAUSE_STATUS, this.getOrderRegion().getCauseStatus())
                .put(Field.COMMENT, this.getOrderRegion().getComment())
                .put(Field.ID_PROJECT, this.getOrderRegion().getIdProject())
                .put(Field.ID_ORDER_CLIENT_EQUIPMENT, this.getOrderRegion().getIdOrderClientEquipment())
                .put(Field.REASSORT, this.getOrderRegion().getReassort())
                .put(Field.ID_OFFER_EQUIPMENT, this.getOrderRegion().getIdOfferEquipment())
                .put(Field.CAMPAIGN, this.getCampaign().toJsonObject())
                .put(Field.CAMPAIGN_NAME, this.getCampaign().getName())
                .put(Field.USE_CREDIT, this.getCampaign().getUse_credit())
                .put(Field.TITRE, this.getTitle())
                .put(Field.ORDER_PARENT, this.getOrderRegionComplex().getOrderClient().toJson())
                .put(Field.BASKET_NAME, this.getBasketOrder().getName())
                .put(Field.BASKET_ID, this.getBasketOrder().getId())
                .put(Field.SECONDE, this.getStudents().getSeconde())
                .put(Field.PREMIERE, this.getStudents().getPremiere())
                .put(Field.TERMINALE, this.getStudents().getTerminale())
                .put(Field.SECONDEPRO, this.getStudents().getSecondepro())
                .put(Field.PREMIEREPRO, this.getStudents().getPremierepro())
                .put(Field.TERMINALEPRO, this.getStudents().getTerminalepro())
                .put(Field.SECONDETECHNO, this.getStudents().getSecondetechno())
                .put(Field.PREMIERETECHNO, this.getStudents().getPremieretechno())
                .put(Field.TERMINALETECHNO, this.getStudents().getTerminaletechno())
                .put(Field.CAP1, this.getStudents().getCap1())
                .put(Field.CAP2, this.getStudents().getCap2())
                .put(Field.CAP3, this.getStudents().getCap3())
                .put(Field.BMA1, this.getStudents().getBma1())
                .put(Field.BMA2, this.getStudents().getBma2())
                .put(Field.PRICEHT, this.priceht)
                .put(Field.TVA5, this.tva5)
                .put(Field.TVA20, this.tva20)
                .put(Field.UNITEDPRICETTC, this.unitedPriceTTC)
                .put(Field.PRICE, this.price)
                .put(Field.TOTALPRICEHT, this.totalPriceHT)
                .put(Field.TOTALPRICETTC, this.totalPriceTTC)
                .put(Field.GRADE, this.grade)
                .put(Field.NAME, this.name)
                .put(Field.IMAGE, this.image)
                .put(Field.EAN, this.ean)
                .put(Field.EDITOR, this.editor)
                .put(Field.DIFFUSOR, this.diffusor)
                .put(Field.TYPE, this.type)
                .put(Field.UAI_STRUCTURE, this.uaiStructure)
                .put(Field.NAME_STRUCTURE, this.nameStructure)
                .put(Field.ADDRESS_STRUCTURE, this.addressStructure)
                .put(Field.EANLDE, this.eanLDE)
                .put(Field.TYPECATALOGUE, this.typeCatalogue);
    }

    public BasketOrder getBasketOrder() {
        return this.orderRegionComplex.getBasketOrder();
    }
}
