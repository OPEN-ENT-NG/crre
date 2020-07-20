import http from 'axios';

export class Userbook {
    id: number;
    name: string;


    async  putPreferences(name,preferences){
        let data = await http.get('/userbook/preference/crre');
        let jsonValue ={};
        if(data.data && data.data.preference)
            jsonValue = JSON.parse(data.data.preference);
        jsonValue[name] = preferences;
        http.put('/userbook/preference/crre',jsonValue);
    }

    async getPreferences(){
         let data = await http.get('/userbook/preference/crre');
         return data.data;
    }
}