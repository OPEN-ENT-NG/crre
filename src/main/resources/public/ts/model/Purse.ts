import { toasts } from 'entcore';
import http from 'axios';
import {Mix, Selectable, Selection} from 'entcore-toolkit';

export class Purse implements Selectable {
    id?: number;
    id_structure: string;
    amount: number;
    selected: boolean;
    substraction?: any;
    bigDifference: boolean;

    constructor (id_structure?: string, amount?: number) {
        if (id_structure) this.id_structure = id_structure;
        if (amount) this.amount = amount;
        this.selected = false;
    }

    async save (): Promise<number> {
        try {
            let {status, data} = await http.put(`/crre/purse/${this.id}`, this.toJson());
            if(status===200) {
                let {amount} = data.amount;
                this.amount = amount;
            }else{
                return status;
            }
        } catch (e) {
            console.log(e)
            toasts.warning('crre.purse.update.err');
        }
    }

    toJson () {
        return {
            id_structure: this.id_structure,
            amount: this.amount
        };
    }
}

export class Purses extends Selection<Purse> {

    constructor () {
        super([]);
    }

    async sync () {
        let {data} = await http.get(`/crre/purses/list`);
        this.all = Mix.castArrayAs(Purse, data);
    }
}

export class PurseImporter {
    files: File[];
    id_campaign: number;
    message: string;

    constructor (id_campaign: number) {
        this.files = [];
        this.id_campaign = id_campaign;
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