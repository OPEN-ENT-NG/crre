import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from "axios";

export interface Title {
    id: number;
    name: string;
}

export class Title implements Selectable {
    selected: boolean;

    constructor(id?: number, name?: string) {
        if (id) {
            this.id = id;
        }
        if (name) {
            this.name = name;
        }
    }
}

export class Titles extends Selection<Title> {

    constructor() {
        super([]);
    }

    async sync(idCampaign: number, idStructure?: string): Promise<void> {
        {
            const uri = idStructure ? `/lystore/titles/campaigns/${idCampaign}/structures/${idStructure}` : `/lystore/titles/campaigns/${idCampaign}`;
            let titles = await http.get(uri);
            this.all = Mix.castArrayAs(Title, titles.data);
        }
    }

    async syncAdmin(idCampaign: number): Promise<void> {
        {
            const uri = `/lystore/titles/campaigns/admin/${idCampaign}`;
            let titles = await http.get(uri);
            this.all = Mix.castArrayAs(Title, titles.data);
        }
    }

    delete(idCampaign: number, idTitle: number, idStructure: string) {
        return http.delete(`/lystore/titles/${idTitle}/campaigns/${idCampaign}/structures/${idStructure}`);
    }
}

export class TitleImporter {
    files: File[];
    id_campaign: number;
    message: string;

    constructor(id_campaign: number) {
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
            response = await http.post(`/lystore/titles/campaigns/${this.id_campaign}/import`,
                formData, {'headers': {'Content-Type': 'multipart/form-data'}});
        }
        catch (err) {
            throw err.response.data;
        }
        return response;
    }
}