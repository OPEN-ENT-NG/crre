package fr.openent.crre.service.impl.bookseller;

import fr.openent.crre.model.config.bookseller.IBooksellerConfigModel;
import fr.openent.crre.service.IBooksellerService;

import java.util.Arrays;

public enum BooksellerServiceEnum {
    CRRE(new CRREBooksellerService());

    private final IBooksellerService<IBooksellerConfigModel> service;

    @SuppressWarnings({"unchecked", "rawtypes"})
    BooksellerServiceEnum(IBooksellerService service) {
        this.service = service;
    }

    public static BooksellerServiceEnum getValue(String value, BooksellerServiceEnum defaultValue) {
        return Arrays.stream(BooksellerServiceEnum.values())
                .filter(booksellerServiceEnum -> booksellerServiceEnum.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(defaultValue);
    }

    public IBooksellerService<IBooksellerConfigModel> getService() {
        return service;
    }
}
