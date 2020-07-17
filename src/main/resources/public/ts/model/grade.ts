import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from "axios";
import {Title} from "./title";

export interface Grade {
    id: number;
    name: string;
}

export class Grade implements Selectable {
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

export class Grades extends Selection<Grade> {
    constructor() {
        super([]);
    }


    async sync(): Promise<void> {
        {
            let grades = await http.get(`/lystore/grades`);
            this.all = Mix.castArrayAs(Title, grades.data);

        }

    }
}