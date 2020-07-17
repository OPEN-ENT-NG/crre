import http from 'axios';
import { notify } from 'entcore';
import { Mix } from 'entcore-toolkit';

export class Log {
    id: number;
    date: string;
    action: string;
    context: string;
    value: any;
    id_user: string;
    username: string;
    item: string;
    selected: boolean;
}

export class Logs {
    all: Log[];
    numberOfPages: number;

    constructor () {
        this.all = [];
    }

    async loadPage (pageNumber: number = 1) {
        try {
            let { data } = await http.get(`/lystore/logs?page=${--pageNumber}`);
            this.all = Mix.castArrayAs(Log, data.logs);
            this.numberOfPages = Math.floor(data.number_logs / 100) + 1;
            this.all.map((log) => log.selected = false);
        } catch (e) {
            notify.error('lystore.logs.sync.err');
        }
    }

    export () {
        location.replace(`/lystore/logs/export`);
    }

    reset () {
        this.all = [];
    }
}