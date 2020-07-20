package fr.openent.crre.service.impl;

import fr.openent.crre.service.EquipmentTypeService;
import org.entcore.common.service.impl.SqlCrudService;

public class DefaultEquipmentType extends SqlCrudService implements EquipmentTypeService {
    public DefaultEquipmentType(String schema, String table) {
        super(schema, table);
    }
}
