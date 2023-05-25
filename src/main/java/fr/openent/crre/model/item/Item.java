package fr.openent.crre.model.item;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.helpers.StringHelper;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Item implements IModel<Item> {
    private String ean;
    private String ark;
    private String title;
    private String editor;
    private String author;
    private String primaryResource;
    private String distributor;
    private String coverUrl;
    private String demoUrl;
    private String publicationDate;
    private String description;
    private String eanPaper;
    private List<String> classes;
    private List<String> disciplines;
    private List<String> levels;
    private List<Availability> availability;
    private List<Tva> tvas;
    private Float priceHT;
    private Offer offers;
    private String type;
    private String catalog;
    private String target;
    private JsonArray technos;
    private String bookSeller;

    public Item(JsonObject json) {
            this.catalog = json.getString(ItemField.TYPE_CATALOG);
            this.bookSeller = json.getString(ItemField.BOOKSELLER);
            this.ean = json.getString(ItemField.EAN);
            this.ark = json.getString(ItemField.ARK);
            this.title = json.getString(ItemField.TITLE);
            this.editor = json.getString(ItemField.EDITOR);
            this.author = json.getString(ItemField.AUTHOR);
            this.primaryResource = json.getString(ItemField.PRIMARY_RESOURCE);
            this.distributor = json.getString(ItemField.DISTRIBUTOR);
            this.coverUrl = json.getString(ItemField.COVER_URL);
            this.demoUrl = json.getString(ItemField.DEMO_URL);
            this.publicationDate = json.getString(ItemField.PUBLICATION_DATE);
            this.description = json.getString(ItemField.DESCRIPTION);
            this.classes = JsonHelper.jsonArrayToList(json.getJsonArray(ItemField.CLASSES, new JsonArray()), JsonObject.class).stream()
                    .filter(c -> c.containsKey(ItemField.LIBELLE))
                    .map(c -> c.getString(ItemField.LIBELLE))
                    .collect(Collectors.toList());
            this.disciplines = JsonHelper.jsonArrayToList(json.getJsonArray(ItemField.DISCIPLINES, new JsonArray()), JsonObject.class).stream()
                    .filter(discipline -> discipline.containsKey(ItemField.LIBELLE))
                    .map(discipline -> discipline.getString(ItemField.LIBELLE))
                    .collect(Collectors.toList());
            this.levels = JsonHelper.jsonArrayToList(json.getJsonArray(ItemField.LEVELS, new JsonArray()), JsonObject.class).stream()
                    .filter(level -> level.containsKey(ItemField.LIBELLE))
                    .map(level -> level.getString(ItemField.LIBELLE))
                    .collect(Collectors.toList());
            this.availability = IModelHelper.toList(json.getJsonArray(ItemField.AVAILABILITY, new JsonArray()), Availability.class);
            this.eanPaper = json.getString(ItemField.EAN_PAPER);
            this.offers = json.getJsonArray(ItemField.OFFERS) != null && !json.getJsonArray(ItemField.OFFERS).isEmpty() ? new Offer(json.getJsonArray(ItemField.OFFERS).getJsonObject(0)) : new Offer();
            this.type = this.catalog.equals(ItemField.PAPER_ITEM) ? json.getString(ItemField.TYPE) : this.offers.getType();
            this.tvas = this.catalog.equals(ItemField.PAPER_ITEM) ? IModelHelper.toList(json.getJsonArray(ItemField.TVAS, new JsonArray()), Tva.class) : this.offers.getTvas();
            this.technos = this.catalog.equals(ItemField.PAPER_ITEM) ? new JsonArray() : json.getJsonArray(ItemField.TECHNOS, new JsonArray());
            this.priceHT = this.catalog.equals(ItemField.PAPER_ITEM) ? json.getFloat(ItemField.PRICE_HT, null) : this.offers.getPriceHT();
            this.target = json.getString(ItemField.TARGET, null);
    }
    
    // Getters and setters

    public String getEan() {
        return ean;
    }

    public Item setEan(String ean) {
        this.ean = ean;
        return this;
    }

    public String getArk() {
        return ark;
    }

    public Item setArk(String ark) {
        this.ark = ark;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Item setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getEditor() {
        return editor;
    }

    public Item setEditor(String editor) {
        this.editor = editor;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Item setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getPrimaryResource() {
        return primaryResource;
    }

    public Item setPrimaryResource(String primaryResource) {
        this.primaryResource = primaryResource;
        return this;
    }

    public String getDistributor() {
        return distributor;
    }

    public Item setDistributor(String distributor) {
        this.distributor = distributor;
        return this;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public Item setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
        return this;
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public Item setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
        return this;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public Item setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Item setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getEanPaper() {
        return eanPaper;
    }

    public Item setEanPaper(String eanPaper) {
        this.eanPaper = eanPaper;
        return this;
    }

    public List<String> getClasses() {
        return classes;
    }

    public Item setClasses(List<String> classes) {
        this.classes = classes;
        return this;
    }

    public List<String> getDisciplines() {
        return disciplines;
    }

    public Item setDisciplines(List<String> disciplines) {
        this.disciplines = disciplines;
        return this;
    }

    public List<String> getLevels() {
        return levels;
    }

    public Item setLevels(List<String> levels) {
        this.levels = levels;
        return this;
    }

    public List<Availability> getAvailability() {
        return availability;
    }

    public Item setAvailability(List<Availability> availability) {
        this.availability = availability;
        return this;
    }

    public Offer getOffers() {
        return offers;
    }

    public Item setOffers(Offer offers) {
        this.offers = offers;
        return this;
    }

    public String getType() {
        return type;
    }

    public Item setType(String type) {
        this.type = type;
        return this;
    }

    public String getCatalog() {
        return catalog;
    }

    public Item setCatalog(String catalog) {
        this.catalog = catalog;
        return this;
    }

    public List<Tva> getTvas() {
        return tvas;
    }

    public Item setTvas(List<Tva> tvas) {
        this.tvas = tvas;
        return this;
    }

    public Float getPriceHT() {
        return priceHT;
    }

    public Item setPriceHT(Float priceHT) {
        this.priceHT = priceHT;
        return this;
    }

    public JsonArray getTechnos() {
        return technos;
    }

    public Item setTechnos(JsonArray technos) {
        this.technos = technos;
        return this;
    }

    public String getBookSeller() {
        return bookSeller;
    }

    public Item setBookSeller(String bookSeller) {
        this.bookSeller = bookSeller;
        return this;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put(ItemField.EAN, this.ean);
        json.put(ItemField.BOOKSELLER, this.bookSeller);
        json.put(Field.ID, this.ean);
        json.put(ItemField.ARK, this.ark);
        json.put(ItemField.TITLE, this.title);
        json.put(ItemField.EDITOR, this.editor);
        json.put(ItemField.AUTHOR, this.author);
        json.put(ItemField.PRIMARY_RESOURCE, this.primaryResource);
        json.put(ItemField.DISTRIBUTOR, this.distributor);
        json.put(ItemField.COVER_URL, this.coverUrl);
        json.put(ItemField.DEMO_URL, this.demoUrl);
        json.put(ItemField.PUBLICATION_DATE, this.publicationDate);
        json.put(ItemField.DESCRIPTION, this.description);
        json.put(ItemField.EAN_PAPER, this.eanPaper);
        json.put(ItemField.TARGET, this.target);

        if (this.classes != null && !this.classes.isEmpty()) {
            JsonArray classesArray = new JsonArray();
            for (String classe : this.classes) {
                JsonObject classeObject = new JsonObject();
                classeObject.put(ItemField.LIBELLE, classe);
                classesArray.add(classeObject);
            }
            json.put(ItemField.CLASSES, classesArray);
        }

        if (this.disciplines != null && !this.disciplines.isEmpty()) {
            JsonArray disciplinesArray = new JsonArray();
            for (String discipline : this.disciplines) {
                JsonObject disciplineObject = new JsonObject();
                disciplineObject.put(ItemField.LIBELLE, discipline);
                disciplinesArray.add(disciplineObject);
            }
            json.put(ItemField.DISCIPLINES, disciplinesArray);
        }

        if (this.levels != null && !this.levels.isEmpty()) {
            JsonArray levelsArray = new JsonArray();
            for (String level : this.levels) {
                JsonObject levelObject = new JsonObject();
                levelObject.put(ItemField.LIBELLE, level);
                levelsArray.add(levelObject);
            }
            json.put(ItemField.LEVELS, levelsArray);
        }

        if (this.availability != null && !this.availability.isEmpty()) {
            json.put(ItemField.AVAILABILITY, IModelHelper.toJsonArray(this.availability));
        }

        if (this.tvas != null && !this.tvas.isEmpty()) {
            json.put(ItemField.TVAS, IModelHelper.toJsonArray(this.tvas));
        }

        if (this.priceHT != null) {
            json.put(ItemField.PRICE_HT, this.priceHT);
        }

        if (this.offers.getLibelle() != null) {
            json.put(ItemField.OFFERS, new JsonArray().add(this.offers.toJson()));
        }

        if (this.type != null) {
            json.put(ItemField.TYPE, this.type);
        }

        if (this.catalog != null) {
            json.put(ItemField.TYPE_CATALOG, this.catalog);
        }

        if(this.technos != null && !this.technos.isEmpty()) {
            json.put(ItemField.TECHNOS, this.technos);
        }

        return json;
    }

    public String getTarget() {
        return target;
    }

    public Item setTarget(String target) {
        this.target = target;
        return this;
    }

    public boolean isValid() {
        return this.ean != null &&
                (this.catalog.equals(ItemField.PAPER_ITEM) || this.getOffers() != null && !this.getOffers().isEmpty()) &&
                !StringHelper.isNullOrEmpty(this.title) &&
                this.tvas != null && !this.tvas.isEmpty() &&
                !StringHelper.isNullOrEmpty(this.type) &&
                !StringHelper.isNullOrEmpty(this.bookSeller) &&
                this.priceHT != null &&
                this.availability != null && !this.availability.isEmpty() &&
                this.availability.stream().allMatch(Availability::isValid);
    }
}
