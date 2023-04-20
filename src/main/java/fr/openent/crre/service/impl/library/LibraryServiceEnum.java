package fr.openent.crre.service.impl.library;

import fr.openent.crre.model.config.library.ILibraryConfigModel;
import fr.openent.crre.service.ILibraryService;

import java.util.Arrays;

public enum LibraryServiceEnum {
    CRRE(new CRRELibraryService());

    private final ILibraryService<ILibraryConfigModel> service;

    @SuppressWarnings({"unchecked", "rawtypes"})
    LibraryServiceEnum(ILibraryService service) {
        this.service = service;
    }

    public static LibraryServiceEnum getValue(String value, LibraryServiceEnum defaultValue) {
        return Arrays.stream(LibraryServiceEnum.values())
                .filter(libraryServiceEnum -> libraryServiceEnum.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(defaultValue);
    }

    public ILibraryService<ILibraryConfigModel> getService() {
        return service;
    }
}
