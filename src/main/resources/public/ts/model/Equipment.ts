import {Tag, Utils} from './index';
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
    tags: Tag[];
    tax_amount: number;
    selected: boolean;
    options: EquipmentOption[];
    deletedOptions?: EquipmentOption[];
    warranty: number;
    catalog_enabled: boolean;
    option_enabled: boolean;
    price_editable: boolean;
    id_type: number;
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
        this.tags = [];
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
            warranty: this.warranty,
            catalog_enabled: this.catalog_enabled,
            option_enabled: this.option_enabled,
            id_type: this.id_type,
            reference : this.reference,
            price_editable: this.price_editable,
            image: this.image || null,
            id_contract: this.id_contract,
            technical_specs:  (this.technical_specs!=null) ? this.technical_specs.map((spec: TechnicalSpec) => spec.toJson()) : [],
            tags: this.tags.map((tag: Tag) => tag.id),
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
            await http.post(`/lystore/equipment`, this.toJson());
        } catch (e) {
            notify.error('lystore.equipment.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/lystore/equipment/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.equipment.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/equipment/${this.id}`);
        } catch (e) {
            notify.error('lystore.equipment.delete.err');
        }
    }

    async sync (id) {
        this.loading = true;

        try {
            let { data } =  await http.get(`/lystore/equipment/${id}`);
            Mix.extend(this, data[0]);
            this.price = parseFloat(this.price.toString());
            this.tax_amount = parseFloat(this.tax_amount.toString());
            this.options.toString() !== '[null]' && this.options !== null ?
                this.options = Mix.castArrayAs(EquipmentOption, JSON.parse(this.options.toString()))
                : this.options = [];
            this.tags.toString() !== '[null]' && this.options !== null ?
                this.tags = Mix.castArrayAs(Tag, JSON.parse(this.tags.toString()))
                : this.tags = [];
            this.technical_specs = this.technical_specs !== null && this.technical_specs.toString() !== '[null]'
                ? Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(this.technical_specs))
                : this.technical_specs;
        } catch (e) {
            console.error(e);
            notify.error('lystore.equipment.sync.err');
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
        this.page = 0;
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
            await http.delete(`/lystore/equipment?${filter}`);
        } catch (e) {
            notify.error('lystore.equipment.delete.err');
        }
    }

    async getPageCount(idCampaign?: number, idStructure?: string) {
        let filter: string = idCampaign && idStructure ? `?idCampaign=${idCampaign}&idStructure=${idStructure}` : '?';
        filter += `${Utils.formatGetParameters({q: this.sort.filters})}`;
        const {data} = await http.get(`/lystore/equipments/pages/count${filter}`);
        this.page_count = data.count;
    }

    async sync(idCampaign?: number, idStructure?: string, page: number = this.page, filter = this.sort) {
        this.loading = true;
        try {
            await this.getPageCount(idCampaign, idStructure);
            const queriesFilter = Utils.formatGetParameters({q: filter.filters});
            const uri: string = idCampaign
                ? `/lystore/equipments/campaign/${idCampaign}?idStructure=${idStructure}&page=${page}&order=${filter.type}&reverse=${filter.reverse}&${queriesFilter}`
                : `/lystore/equipments?page=${page}&order=${filter.type}&reverse=${filter.reverse}&${queriesFilter}`;
            let {data} = await http.get(uri);
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.price = parseFloat(equipment.price.toString());
                equipment.tax_amount = parseFloat(equipment.tax_amount.toString());
                if (idCampaign) {
                    equipment.tags = equipment.tags !== null && equipment.tags.toString() !== '[null]' ? JSON.parse(equipment.tags.toString()) : [];
                    equipment.options.toString() !== '[null]' && equipment.options !== null ?
                        equipment.options = Mix.castArrayAs(EquipmentOption, JSON.parse(equipment.options.toString()))
                        : equipment.options = [];
                    equipment.technical_specs = equipment.technical_specs !== null
                        ? Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(equipment.technical_specs.toString()))
                        : equipment.technical_specs;
                }
            });

        } catch (e) {
            notify.error('lystore.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    async syncAll(idCampaign: number, idStructure?: string) {
        try {
            const uri: string = idStructure
                ? `/lystore/equipments/admin/${idCampaign}?idStructure=${idStructure}`
                : `/lystore/equipments/admin/${idCampaign}`;
            let {data} = await http.get(uri);
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.price = parseFloat(equipment.price.toString());
                equipment.tax_amount = parseFloat(equipment.tax_amount.toString());
                equipment.priceTTC = equipment.price + (equipment.price * equipment.tax_amount / 100);
                if (idCampaign) {
                    equipment.tags = equipment.tags !== null && equipment.tags.toString() !== '[null]' ? JSON.parse(equipment.tags.toString()) : [];
                    equipment.options.toString() !== '[null]' && equipment.options !== null ?
                        equipment.options = Mix.castArrayAs(EquipmentOption, JSON.parse(equipment.options.toString()))
                        : equipment.options = [];
                    equipment.technical_specs = equipment.technical_specs !== null
                        ? Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(equipment.technical_specs.toString()))
                        : equipment.technical_specs;
                }
            });

        } catch (e) {
            notify.error('lystore.equipment.sync.err');
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

    loadNext(idCampaign?: number, idStructure?: string, filter?: { type: string, reverse: boolean, filters: string[] }) {
        return this.sync(idCampaign, idStructure, ++this.page, filter);
    }

    loadPrev(idCampaign?: number, idStructure?: string, filter?: { type: string, reverse: boolean; filters: string[] }) {
        return this.sync(idCampaign, idStructure, --this.page, filter);
    }

    async setStatus (status: string): Promise<void> {
        try {
            let params = Utils.formatKeyToParameter(this.selected, 'id');
            await http.put(`/lystore/equipments/${status}?${params}`);
        } catch (e) {
            notify.error('lystore.equipment.update.err');
            throw e;
        }
    }

    async search(text: String, fieldName: String) {
        try {
            if ((text.trim() === '' || !text) || (fieldName.trim() === '' || !fieldName)) return;
            const {data} = await http.get(`/lystore/equipments/search?q=${text}&field=${fieldName}`);
            return Mix.castArrayAs(Equipment, data);
        } catch (err) {
            notify.error('lystore.option.search.err');
            throw err;
        }
    }

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
                response = await http.post(`/lystore/equipments/contract/${this.id_contract}/import`,
                    formData, {'headers': {'Content-Type': 'multipart/form-data'}});
                return response;
            } catch (err) {
                throw err.response.data;
            }
        } else throw new Error("lystore.equipment.import.contract");
    }
}