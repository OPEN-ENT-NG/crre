import http from 'axios';
import {_, moment, toasts} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {IFilter, StructureGroup} from './index';


export class Campaign implements Selectable, IFilter  {
    id?: number;
    name: string;
    description: string;
    accessible: boolean;
    groups: StructureGroup[];
    selected: boolean;
    purse_amount?: number;
    consumable_purse_amount?: number;
    initial_purse_amount?: number;
    consumable_initial_purse_amount?: number;
    nb_structures: number;
    nb_panier?: number;
    purse_enabled: boolean;
    priority_enabled: boolean;
    reassort: boolean;
    catalog: string;
    structure: string;
    priority_field: null|PRIORITY_FIELD;
    end_date :Date;
    start_date : Date;
    automatic_close : boolean;
    use_credit : string;
    id_type: number;

    constructor (name?: string, description?: string) {
        if (name) this.name = name;
        if (description) this.description = description;
        this.groups = [];
        this.purse_enabled = true;
        this.priority_enabled = false;
        this.priority_field = PRIORITY_FIELD.ORDER;
        this.reassort = false;
        this.automatic_close = false;
        this.id_type = null;
    }

    toJson () {
        return {
            name: this.name,
            description: this.description || null,
            accessible: this.accessible || (moment(this.start_date).diff(moment()) <= 0 && moment(this.end_date).diff(moment()) >= 0),
            groups: this.groups.map((group) => {
                return group.toJson();
            }),
            purse_enabled: this.purse_enabled,
            priority_enabled: this.priority_enabled,
            priority_field: this.priority_field,
            reassort: this.reassort,
            catalog: this.catalog,
            end_date : this.end_date,
            start_date: this.start_date,
            automatic_close: this.automatic_close,
            use_credit : this.use_credit,
            id_type: this.id_type
        };
    }

    reformatCampaign() {
        if (this.end_date != null && this.start_date != null) {
            this.accessible = this.accessible
                ||
                (moment(this.start_date).diff(moment()) <= 0
                    && moment(this.end_date).diff(moment()) >= 0);
            this.start_date = moment(this.start_date);
            this.end_date = moment(this.end_date);
        }
        if (this.groups[0] !== null) {
            this.groups = Mix.castArrayAs(StructureGroup, JSON.parse(this.groups.toString()));
        } else this.groups = [];
    }

    async save () {
        if (this.id) {
            if(this.automatic_close)
                this.accessible = false;
            await this.update();
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/crre/campaign`, this.toJson());
        } catch (e) {
            toasts.warning('crre.campaign.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/crre/campaign/${this.id}`, this.toJson());
        } catch (e) {
            toasts.warning('crre.campaign.update.err');
        }
    }

    async delete () {
        try {
            await http.delete(`/crre/campaign/${this.id}`);
        } catch (e) {
            toasts.warning('crre.campaign.delete.err');
        }
    }
    async updateAccessibility() {
        try {
            await http.put(`/crre/campaign/accessibility/${this.id}`, this.toJson());
        } catch (e) {
            toasts.warning('crre.campaign.update.err');
        }
    }

    async sync (id) {
        try {
            let { data } = await http.get(`/crre/campaigns/${id}`);
            Mix.extend(this, Mix.castAs(Campaign, data));
            this.reformatCampaign();
        } catch (e) {
            toasts.warning('crre.campaign.sync.err');
        }
    }

    getValue(): string {
        return this.id.toString();
    }

    getKey(): string {
        return "id_campaign";
    }
}

export class Campaigns extends Selection<Campaign> {

    constructor () {
        super([]);
    }

    async delete (campaigns: Campaign[]): Promise<void> {
        try {
            let filter = '';
            campaigns.map((campaign) => filter += `id=${campaign.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/crre/campaign?${filter}`);
        } catch (e) {
            toasts.warning('crre.campaign.delete.err');
        }
    }

    async sync (Structure?: string) {
        try {
            let { data } = await http.get( Structure ? `/crre/campaigns?idStructure=${Structure}`  : `/crre/campaigns`  );
            this.all = Mix.castArrayAs(Campaign, data);
            this.all.forEach(c => {c.reformatCampaign();});
        } catch (e) {
            toasts.warning('crre.campaigns.sync.err');
        }
    }

    get (idCampaign: number): Campaign {
        return _.findWhere(this.all, { id: idCampaign });
    }

    isEmpty (): boolean {
        return this.all.length === 0;
    }
}

export enum PRIORITY_FIELD {
    PROJECT = 'PROJECT',
    ORDER = 'ORDER'
}