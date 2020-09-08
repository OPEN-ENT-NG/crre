import {Utils} from './index';
import {_, notify} from 'entcore';
import {Eventer, Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Equipment implements Selectable {
    id?: number;
    name: string;
    summary: string;
    description: string;
    price: number;
    id_tax: number;
    id_contract: number;
    status: string;
    image: string;
    reference: string;
    id_option?: number;
    technical_specs: TechnicalSpec[];
    tax_amount: number;
    selected: boolean;
    options: EquipmentOption[];
    deletedOptions?: EquipmentOption[];
    catalog_enabled: boolean;
    option_enabled: boolean;
    price_editable: boolean;
    eventer: Eventer;
    _loading: boolean;
    priceTTC?: number;
    contract_type_name:string;

    constructor (name?: string, price?: number) {
        this.eventer = new Eventer();
        this._loading = false;
        if (name) this.name = name;
        if (price) this.price = price;
        this.technical_specs = [];
        this.options = [];
        this.price_editable = false;
        this.option_enabled = false;
        this.catalog_enabled = true;
    }

    toString(): string {
        return `${this.reference} - ${this.name}`;
    }

    toJson () {
        let optionList =  this.options.map((option: EquipmentOption) => option.toJson());
        return {
            name: this.name,
            summary: this.summary || null,
            description: this.description || null,
            price: parseFloat(this.price.toString()),
            id_tax: this.id_tax,
            status: this.status,
            catalog_enabled: this.catalog_enabled,
            option_enabled: this.option_enabled,
            reference : this.reference,
            price_editable: this.price_editable,
            image: this.image || null,
            id_contract: this.id_contract,
            technical_specs:  (this.technical_specs!=null) ? this.technical_specs.map((spec: TechnicalSpec) => spec.toJson()) : [],
            optionsCreate : _.filter(optionList, function(option) { return option.id === null ; }) ,
            optionsUpdate : _.filter(optionList, function(option) { return option.id !== null ; }) ,
            deletedOptions : this.deletedOptions || null,
        };
    }

    async save () {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/crre/equipment`, this.toJson());
        } catch (e) {
            notify.error('crre.equipment.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/crre/equipment/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('crre.equipment.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            await http.delete(`/crre/equipment/${this.id}`);
        } catch (e) {
            notify.error('crre.equipment.delete.err');
        }
    }

    async sync (id) {
        this.loading = true;

        try {
            let { data } =  await http.get(`/crre/equipment/${id}`);
            Mix.extend(this, data[0]);
            this.price = parseFloat(this.price.toString());
            this.tax_amount = parseFloat(this.tax_amount.toString());
            this.options.toString() !== '[null]' && this.options !== null ?
                this.options = Mix.castArrayAs(EquipmentOption, JSON.parse(this.options.toString()))
                : this.options = [];
            this.technical_specs = this.technical_specs !== null && this.technical_specs.toString() !== '[null]'
                ? Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(this.technical_specs))
                : this.technical_specs;
        } catch (e) {
            console.error(e);
            notify.error('crre.equipment.sync.err');
        }
        finally {
            this.loading = false;
        }
    }

    set loading(state: boolean) {
        this._loading = state;
        this.eventer.trigger(`loading::${this._loading}`);
    }

    get loading() {
        return this._loading;
    }

}

export class TechnicalSpec {
    name: string;
    value: string;
    constructor(){
    }
    toJson () {
        return {
            name: this.name,
            value: this.value
        };
    }
    toString () {
        return this.name + ' ' + this.value;
    }
}

export interface Equipments {
    eventer: Eventer;
    page: number;
    _loading: boolean;
    all: Equipment[];
    page_count: number;
    subjects: String[];
    grades: String[];
    editors: String[];

    sort: {
        type: string,
        reverse: boolean,
        filters: string[]
    }
}

export class Equipments extends Selection<Equipment> {
    constructor() {
        super([]);
        this.eventer = new Eventer();
        this.subjects = [];
        this.grades = [];
        this._loading = false;
        this.sort = {
            type: 'name',
            reverse: false,
            filters: []
        };
    }

    async delete (equipments: Equipment[]): Promise<void> {
        try {
            let filter = '';
            equipments.map((equipment) => filter += `id=${equipment.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/crre/equipment?${filter}`);
        } catch (e) {
            notify.error('crre.equipment.delete.err');
        }
    }

    async syncEquip (data: any) {
        this.all = Mix.castArrayAs(Equipment, data);
        this.all.map((equipment) => {
            equipment.price = parseFloat(equipment.price.toString());
            // equipment.tax_amount = parseFloat(equipment.tax_amount.toString());
            // this.equipmentsOptionsTechnicalsSpecs(true, equipment);
        });
    }

    async getFilterEquipments(word: string, filter?: string){
        this.loading = true;
        try {
            let uri: string;
            if(filter) {
                uri = (`/crre/equipments/catalog/filter?filter=${filter}&word=${word}`);
            } else {
                uri = (`/crre/equipments/catalog/search?word=${word}`);
            }
            let {data} = await http.get(uri);
            this.syncEquip(data);
        } catch (e) {
            notify.error('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    async getFilters(){
        const {data} = await http.get(`/crre/equipments/filters`);
        this.subjects = data.subjects;
        this.grades = data.grades;
        this.editors = data.editors;
    }

    async sync() {
        this.loading = true;
        try {
            // old
            // const queriesFilter = Utils.formatGetParameters({q: filter.filters});
            // let uri = `/crre/equipments?order=${filter.type}&reverse=${filter.reverse}&${queriesFilter}`;
            
            let {data} = await http.get(`/crre/equipments/catalog`);
            this.syncEquip(data);

        } catch (e) {
            notify.error('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    private equipmentsOptionsTechnicalsSpecs(isCatalog:boolean, equipment: Equipment) {
        if(isCatalog) {
            equipment.options.toString() !== '[null]' && equipment.options !== null ?
                equipment.options = Mix.castArrayAs(EquipmentOption, JSON.parse(equipment.options.toString()))
                : equipment.options = [];
            equipment.technical_specs = equipment.technical_specs !== null
                ? Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(equipment.technical_specs.toString()))
                : equipment.technical_specs;
        }
    }

    async syncAll(idStructure?: string) {
        try {
            const uri: string = idStructure
                ? `/crre/equipments/admin/?idStructure=${idStructure}`
                : `/crre/equipments/admin/`;
            let {data} = await http.get(uri);
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.price = parseFloat(equipment.price.toString());
                equipment.tax_amount = parseFloat(equipment.tax_amount.toString());
                equipment.priceTTC = equipment.price + (equipment.price * equipment.tax_amount / 100);
                // this.equipmentsOptionsTechnicalsSpecs(false, equipment);
            });

        } catch (e) {
            notify.error('crre.equipment.sync.err');
            throw e;

        }
    }

    set loading(state: boolean) {
        this._loading = state;
        this.eventer.trigger(`loading::${this._loading}`);
    }

    get loading() {
        return this._loading;
    }

    async setStatus (status: string): Promise<void> {
        try {
            let params = Utils.formatKeyToParameter(this.selected, 'id');
            await http.put(`/crre/equipments/${status}?${params}`);
        } catch (e) {
            notify.error('crre.equipment.update.err');
            throw e;
        }
    }
    
/*    async search(text: String, fieldName: String) {
        try {
            if ((text.trim() === '' || !text) || (fieldName.trim() === '' || !fieldName)) return;
            const {data} = await http.get(`/crre/equipments/search?q=${text}&field=${fieldName}`);
            return Mix.castArrayAs(Equipment, data);
        } catch (err) {
            notify.error('crre.option.search.err');
            throw err;
        }
    }*/

}

export class EquipmentOption implements Selectable {

    id?: number;
    amount: number;
    required: boolean;
    selected: boolean;
    id_option: number;
    search?: Equipment[];
    searchReference?: Equipment[];


    constructor () {
        this.amount = 1;
        this.required = true;
    }

    toJson () {
        return {
            id: this.id ? this.id : null,
            amount: parseInt(this.amount.toString()),
            required : this.required,
            id_option: this.id_option
        };
    }

}

export class EquipmentImporter {
    files: File[];
    eventer: Eventer;
    message: string;
    id_contract?: number;
    _loading: boolean;
    err: boolean;

    constructor() {
        this.files = [];
        this.eventer = new Eventer();
        this._loading = false;
    }

    isValid(): boolean {
        if (this.id_contract && this.id_contract >= 0) {
            return this.files.length > 0
                ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
                : false;
        }
    }

    set loading(value) {
        this._loading = value;
        this.eventer.trigger(`loading::${value}`);
    }

    get loading() {
        return this._loading;
    }

    async validate(): Promise<any> {
        this.loading = true;
        try {
            await this.postFile();
        } catch (err) {
            this.err = err;
            throw err;
        }
        finally {
            this.loading = false;
        }
    }

    private async postFile(): Promise<any> {
        if (this.id_contract) {
            let formData = new FormData();
            formData.append('file', this.files[0], this.files[0].name);
            let response;
            try {
                response = await http.post(`/crre/equipments/contract/${this.id_contract}/import`,
                    formData, {'headers': {'Content-Type': 'multipart/form-data'}});
                return response;
            } catch (err) {
                throw err.response.data;
            }
        } else throw new Error("crre.equipment.import.contract");
    }
}