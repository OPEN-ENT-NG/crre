package fr.openent.crre.service.impl;

import fr.openent.crre.service.TaxService;
import org.entcore.common.service.impl.SqlCrudService;

public class DefaultTaxService extends SqlCrudService implements TaxService {
    public DefaultTaxService(String schema, String table) {
        super(schema, table);
    }
}
