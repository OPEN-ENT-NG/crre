import { notify,toasts } from 'entcore';
import http from 'axios';
import {Mix, Selectable, Selection} from 'entcore-toolkit';

export class Purse implements Selectable {
    id?: number;
    id_structure: string;
    amount: number;
    id_campaign: number;

    selected: boolean;
    substraction?: any;
    bigDifference: boolean;

    constructor (id_structure?: string, amount?: number, id_campaign?: number) {
        if (id_structure) this.id_structure = id_structure;
        if (amount) this.amount = amount;
        if (id_campaign) this.id_campaign = id_campaign;

        this.selected = false;
    }

    async save (): Promise<number> {
        try {
            let {status, data} = await http.put(`/lystore/purse/${this.id}`, this.toJson());
            if(status===200) {
                let {amount} = data.amount;
                this.amount = amount;
            }else{
                return status;
            }
        } catch (e) {
            console.log(e)
            notify.error('lystore.purse.update.err');
        }
    }

    toJson () {
        return {
            id_structure: this.id_structure,
            amount: this.amount,
            id_campaign: this.id_campaign
        };
    }
}

export class Purses extends Selection<Purse> {

    id_campaign: number;
    constructor (id_campaign: number) {
        super([]);
        this.id_campaign = id_campaign;
    }

    async sync () {
        let {data} = await http.get(`/lystore/campaign/${this.id_campaign}/purses/list`);
        this.all = Mix.castArrayAs(Purse, data);
    }

    async check(id_campaign){
        let {data, status} =  await  http.get(`/lystore/campaign/${id_campaign}/purse/check`);
        if(status===201){
            this.all.map(purse => {
                data.map( back_data =>{
                    if (back_data.id_structure && back_data.id_structure === purse.id_structure){
                        purse.substraction = back_data.substraction;
                        if(purse.substraction !== 0){
                            if(Math.abs(purse.substraction) < 2){
                                purse.bigDifference = false;
                            }else{
                                purse.bigDifference = true;
                            }
                        }
                    }
                })
            })
        }
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
            response = await http.post(`/lystore/campaign/${this.id_campaign}/purses/import`,
                formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
        } catch (err) {
            throw err.response.data;
        }
        return response;
    }
}