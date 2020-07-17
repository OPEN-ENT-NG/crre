import { Provider } from 'entcore-toolkit';
import { notify } from 'entcore';

export class EquipmentType {
    id: number;
    name: string;
}

export class EquipmentTypes {
    all: EquipmentType[];
    mapping: {};
    provider: Provider<EquipmentType>;

    constructor(){
        this.all = [];
        this.mapping = {};
        this.provider = new Provider<EquipmentType>('lystore/equipmentType',EquipmentType);
    }

    async sync (force : boolean = false){
        try{
            if (this.provider.isSynced) this.provider.isSynced = !force;
            this.all = await this.provider.data();
            this.all.map((equipmenttype) => this.mapping[equipmenttype.id] = equipmenttype);
        }catch (e) {
            notify.error('lystore.equipment_type.sync.error');
            throw e;
        }
    }
}