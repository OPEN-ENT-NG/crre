import {moment, notify} from 'entcore';
import {Mix, Provider, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Contract implements Selectable {
    id?: number;
    name: string;
    reference: string;
    annual_min: number | string;
    annual_max: number | string;
    start_date: string | Date;
    nb_renewal: string;
    id_contract_type: number;
    max_brink: number | string;
    id_supplier: number;
    id_agent: number;
    end_date: string;
    renewal_end: string;
    supplier_display_name: string;
    file: boolean;

    selected: false;
    annual_min_enabled: boolean;
    annual_max_enabled: boolean;
    max_brink_enabled: boolean;

    constructor (name?: string, reference?: string, annual_min?: number, annual_max?: number,
                 start_date?: string | Date, max_brink?: number) {
        if ( name ) this.name = name;
        if ( reference ) this.reference = reference;
        if ( annual_min ) {
            this.annual_min = annual_min;
            this.annual_min_enabled = true;
        }
        if ( annual_max ) {
            this.annual_max = annual_max;
            this.annual_max_enabled = true;
        }
        if ( max_brink ) {
            this.max_brink = max_brink;
            this.max_brink_enabled = true;
        }
        if ( start_date ) this.start_date = start_date;
        this.file = false;
    }

    toJson() {
        return {
            name: this.name,
            reference: this.reference,
            start_date: moment(this.start_date).format('YYYY-MM-DD'),
            nb_renewal: parseInt(this.nb_renewal),
            id_contract_type: this.id_contract_type,
            id_supplier: this.id_supplier,
            id_agent: this.id_agent,
            end_date: moment(this.end_date).format('YYYY-MM-DD'),
            renewal_end: moment(this.end_date).add(this.nb_renewal, 'y').format('YYYY-MM-DD'),
            annual_min: this.annual_min_enabled && this.annual_min !== null ? parseFloat((this.annual_min as number).toString()) : null,
            annual_max: this.annual_max_enabled && this.annual_max !== null ? parseFloat((this.annual_max as number).toString()) : null,
            max_brink: this.max_brink_enabled && this.max_brink !== null ? parseFloat((this.max_brink as number).toString()) : null,
            file: this.file
        };
    }


    async save (): Promise<void> {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create (): Promise<void> {
        try {
            let res = await http.post(`/lystore/contract`, this.toJson());
            this.id = res.data.id;
        } catch (e) {
            notify.error('lystore.contract.create.err');
        }
    }

    async update (): Promise<void> {
        try {
            await http.put(`/lystore/contract/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.contract.update.err');
        }
    }

    async delete (): Promise<void> {
        try {
            await http.delete(`/lystore/contract?id=${this.id}`);
        } catch (e) {
            notify.error('lystore.contract.delete.err');
        }
    }
    syncBooleans = async () => {
        this.annual_min_enabled = this.annual_min !== undefined && this.annual_min !== null ;
        this.annual_max_enabled = this.annual_max !== undefined && this.annual_max !== null;
        this.max_brink_enabled = this.max_brink !== undefined && this.max_brink !== null;
    }
}

export class Contracts extends Selection<Contract> {
    provider: Provider<Contract>;
    mapping: {};

    constructor () {
        super([]);
        this.provider = new Provider(`/lystore/contracts`, Contract);
        this.mapping = {};
    }

    async sync (force: boolean) {
        if (this.provider.isSynced) this.provider.isSynced = !force;
        this.all = await this.provider.data();
        this.all.map((contract) => this.mapping[contract.id] = Mix.castAs(Contract, contract) );
    }

    async delete (contracts: Contract[]): Promise<void> {
        try {
            let filter = '';
            contracts.map((contract) => filter += `id=${contract.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/contract?${filter}`);
        } catch (e) {
            notify.error('lystore.contract.delete.err');
        }
    }

    get (id: number) {
        return this.mapping[id];
    }
}