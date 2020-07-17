package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.EquipmentTypeService;
import org.entcore.common.service.impl.SqlCrudService;

public class DefaultEquipmentType extends SqlCrudService implements EquipmentTypeService {
    public DefaultEquipmentType(String schema, String table) {
        super(schema, table);
    }
}
