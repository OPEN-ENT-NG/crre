import http from 'axios';
import { Selectable, Selection, Mix } from 'entcore-toolkit';

export class Program implements Selectable {
    id?: number;
    name: string;

    selected: boolean;

    contructor (name: string) {
        this.name = name;
        this.selected = false;
    }
}

export class Programs extends Selection<Program> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let programs = await http.get(`/lystore/programs`);
        this.all = Mix.castArrayAs(Program, programs.data);
    }
}