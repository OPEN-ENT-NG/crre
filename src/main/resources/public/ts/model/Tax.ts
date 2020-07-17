import { Provider } from 'entcore-toolkit';
import { notify } from 'entcore';

export class Tax {
    id?: number;
    name: string;
    value: string;
}

export class Taxes {
    all: Tax[];
    mapping: {};
    provider: Provider<Tax>;

    constructor () {
        this.all = [];
        this.mapping = {};
        this.provider = new Provider<Tax>(`/lystore/taxes`, Tax);
    }

    async sync (force: boolean = false) {
        try {
            if (this.provider.isSynced) this.provider.isSynced = !force;
            this.all = await this.provider.data();
            this.all.map((tax) => this.mapping[tax.id] = tax);
        } catch (e) {
            notify.error('lystore.tax.sync.err');
            throw e;
        }
    }
}