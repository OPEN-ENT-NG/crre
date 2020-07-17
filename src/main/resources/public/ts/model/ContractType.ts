import http from 'axios';
import { Selectable, Selection, Mix } from 'entcore-toolkit';

export class ContractType implements Selectable {
    id?: number;
    code: string;
    name: string;
    displayName: string;

    selected: boolean;

    constructor (code?: string, name?: string) {
        if (code) this.code = code;
        if (name) this.name = name;

        this.selected = false;
    }
}

export class ContractTypes extends Selection<ContractType> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let types = await http.get(`/lystore/contract/types`);
        this.all = Mix.castArrayAs(ContractType, types.data);
        this.all.map((type) => type.displayName = type.code + ' - ' + type.name);
    }
}