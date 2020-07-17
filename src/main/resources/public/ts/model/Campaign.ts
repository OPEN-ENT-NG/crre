import http from 'axios';
import {_, notify} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {Purses, StructureGroup, Tags, Titles} from './index';


export class Campaign implements Selectable  {
    id?: number;
    name: string;
    description: string;
    image: string;
    accessible: boolean;
    groups: StructureGroup[];
    selected: boolean;
    purse_amount?: number;
    initial_purse_amount?: number;
    nb_structures: number;
    nb_equipments: number;
    purses?: Purses;
    titles: Titles;
    nb_panier?: number;
    purse_enabled: boolean;
    priority_enabled: boolean;
    priority_field: null|PRIORITY_FIELD;

    constructor (name?: string, description?: string) {
        if (name) this.name = name;
        if (description) this.description = description;
        this.groups = [];
        this.image = '';
        this.purse_enabled = false;
        this.priority_enabled = true;
        this.priority_field = PRIORITY_FIELD.ORDER
    }

    toJson () {
        return {
            name: this.name,
            description: this.description || null,
            image: this.image || null,
            accessible: this.accessible || false,
            groups: this.groups.map((group) => {
                return group.toJson();
            }),
            purse_enabled: this.purse_enabled,
            priority_enabled: this.priority_enabled,
            priority_field: this.priority_field
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
            await http.post(`/lystore/campaign`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/lystore/campaign/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.update.err');
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/campaign/${this.id}`);
        } catch (e) {
            notify.error('lystore.campaign.delete.err');
        }
    }
    async updateAccessibility() {
        try {
            await http.put(`/lystore/campaign/accessibility/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.update.err');
        }
    }
    projectPriorityEnable(){
        return (this.priority_field == PRIORITY_FIELD.PROJECT || !this.priority_field )  && this.priority_enabled ;
    }
    orderPriorityEnable(){
        return this.priority_field == PRIORITY_FIELD.ORDER  && this.priority_enabled ;
    }
    async sync (id, tags?: Tags) {
        try {
            let { data } = await http.get(`/lystore/campaigns/${id}`);
            Mix.extend(this, Mix.castAs(Campaign, data));
            if (this.groups[0] !== null ) {
                this.groups = Mix.castArrayAs(StructureGroup, JSON.parse(this.groups.toString())) ;
                if (tags) {
                this.groups.map((group) => {
                    group.tags =  group.tags.map( (tag) => {
                        return _.findWhere(tags, {id: tag});
                    });
                });
                }
            } else this.groups = [];

        } catch (e) {
            notify.error('lystore.campaign.sync.err');
        }
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
            await http.delete(`/lystore/campaign?${filter}`);
        } catch (e) {
            notify.error('lystore.campaign.delete.err');
        }
    }

    async sync (Structure?: string) {
        try {
            let { data } = await http.get( Structure ? `/lystore/campaigns?idStructure=${Structure}`  : `/lystore/campaigns`  );
            this.all = Mix.castArrayAs(Campaign, data);
        } catch (e) {
            notify.error('lystore.campaigns.sync.err');
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