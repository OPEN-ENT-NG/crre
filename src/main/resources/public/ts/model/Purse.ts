import { toasts } from 'entcore';
import http from 'axios';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {Utils} from "./Utils";

declare let window: any;
export class Purse implements Selectable {
    id?: number;
    id_structure: string;
    amount: number;
    initial_amount: number;
    selected: boolean;
    seconde: number;
    premiere: number;
    terminale: number;
    total: number;
    licence_amount: number;
    licence_initial_amount: number;

    constructor (id_structure?: string, amount?: number) {
        if (id_structure) this.id_structure = id_structure;
        if (amount) this.amount = amount;
        this.selected = false;
    }

    async save (): Promise<void> {
        try {
            await http.put(`/crre/purse/${this.id_structure}`, this.toJson());
        } catch (e) {
            console.log(e)
            toasts.warning('crre.purse.update.err');
        }
    }

    toJson () {
        return {
            id_structure: this.id_structure,
            initial_amount: this.initial_amount,
            licence_initial_amount: this.licence_initial_amount
        };
    }
}

export class Purses extends Selection<Purse> {

    constructor () {
        super([]);
    }

    async get (page?:number) {
        const pageParams = (page) ? `?page=${page}` : ``;
        let {data} = await http.get(`/crre/purses/list${pageParams}`);
        return Mix.castArrayAs(Purse, data);
    }

    async search (name: string, page?:number) {
        let {data} =  await http.get(`/crre/purse/search?q=${name}&page=${page}`);
        return Mix.castArrayAs(Purse, data);
    }

    exportPurses() {
        let params_id_purses = Utils.formatKeyToParameter(this.selected, 'id');
        window.location = `/crre/purses/export?${params_id_purses}`;
    };
}

export class PurseImporter {
    files: File[];
    message: string;

    constructor () {
        this.files = [];
    }

    isValid(): boolean {
        return this.files.length > 0
            ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
            : false;
    }

    async validate(): Promise<any> {
        try {
            await this.postFile();
        } catch (err) {
            throw err;
        }
    }

    private async postFile(): Promise<any> {
        let formData = new FormData();
        formData.append('file', this.files[0], this.files[0].name);
        let response;
        try {
            response = await http.post(`/crre/purses/import`,
                formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
        } catch (err) {
            throw err.response.data;
        }
        return response;
    }
}