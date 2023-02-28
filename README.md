# À propos de l'application CRRE
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt)
* Développeur : CGI
* Financeur : Région Ile de France
* Description : Le Catalogue de Ressources Régionales Educatives est un service de gestion des achats des ressources numériques

# Présentation du module

L'application **CRRE**, mise à disposition des collèges et lycées d'île-de-France, permet de gérer les différentes campagnes d'équipements en ressources numériques issus de marchés publics en s'appuyant sur un catalogue. Elle propose deux types de campagnes :

 - Les campagnes basées sur un panier/commande et une enveloppe budgétaire/nombre de licences annuelle allouée aux établissements
 - Les campagnes basées sur un panier/demande organisé sous forme d'une liste de voeux avec instruction de la demande

## Fonctionnalités

4 profils utilisateurs sont disponibles :
 - Le profil Enseignant/Personnel lambda qui peut consulter les ressources numériques proposées dans le catalogue
 - Le profil Prescripteur qui peut choisir une campagne, se constituer un panier et le soumettre à validation. Il peut également suivre l'état de ses demandes
 - Le profil Valideur qui voit la liste des demandes à valider et qui peut suivre l'ensemble des demandes de l'établissement
 - Le profil Administrateur qui voit la liste des demandes à traiter, génère les bons de commande, clôture les demandes. Il gère également les campagnes, les regroupements d'établissements et éventuellement le catalogue
 - Le profil Libraire qui gère le catalogue et l'état de chacun des articles.

## Configuration

<pre>
{
   "name":"fr.openent~crre~1.1.8",
   "config":{
      "main":"fr.openent.crre.Crre",
      "port":8150,
      "app-name":"CRRE",
      "app-address":"/crre",
      "app-icon":"${host}/crre/public/img/logo.png",
      "app-type":"END_USER",
      "host":"${host}",
      "ssl":$ssl,
      "auto-redeploy":false,
      "userbook-host":"${host}",
      "integration-mode":"HTTP",
      "app-registry.port":8012,
      "mode":"${mode}",
      "entcore.port":8009,
      "sql":true,
      "db-schema":"crre",
      "mail":{
         "address":"${address}"
      },
      "elasticsearch":true,
      "elasticsearchConfig":{
         "server-uri":"${elasticServerURI}",
         "index":"${elasticCRREIndexName}"
      },
      "timeSecondSynchCron":"${timeSecondSynchCron}",
      "timeSecondStatCron":"${timeSecondStatCron}",
      "timeSecondStatutCron":"${timeSecondStatutCron}",
      "timeSecondNotifyAdminsCron":"${timeSecondNotifyAdminsCron}",
      "encodeEmailContent":"${crreEncodeEmailContent}"
   }
}
</pre>
Dans votre springboard, vous devez inclure des variables d'environnement :

| **conf.properties**       | **Utilisation**                                                                                                                                                                                 | **Exemple**                     |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| "${address}"              | adresse e-mail du libraire                                                                                                                                                                      | xxx@yyy.com                     |
| "${elasticServerURI}"     | URI du serveur Elasticsearch                                                                                                                                                                    | http://XXX.XXX.XXX.XXX:XXXXX    |
| "${elasticCRREIndexName}" | Nom de l'indice Elasticsearch pour CRRE                                                                                                                                                         | /articlenumerique,articlepapier |
| "${timeSecondSynchCron}"  | Cron pour la récupération des statistiques de tous les estabs (nombre de commandes d'articles numériques, papier, crédits dépensés...) et injection dans la base de données Mongo pour stockage. | */30 * * * * ? *                |
| "${timeSecondStatCron}"   | Cron pour mettre à jour le statut des commandes dans BDD par rapport aux informations transmises par LDE.                                                                                       | */30 * * * * ? *                |
| "${timeSecondStatutCron}" | Cron pour récupérer le nombre d'élèves de chaque filière (seconde à terminale, cap, bma....) de chaque établissement et mettre à jour les effectifs de chaque établissement en BDD.             | */30 * * * * ? *                |
| "${timeSecondNotifyAdminsCron}" | Cron pour envoyer une notification aux admins chaque soir avec le nombre de commande reçue.                                                                                                     | */30 * * * * ? *                |
| "${crreEncodeEmailContent}" | Encode le contenu des mail en base64 avant l'envois. Utilisez pour le serveur SendingBlue de ODE                                                                                                | true                            |

De base CRRE va prendre le service mail défini dans la config de l'infra
On peut redéfinir ce paramétrage en rajoutant une config dans la conf de CRRE

<pre>
{
   "name":"fr.openent~crre~1.1.8",
   "config":{
        ...
        "emailConfig": ${crreEmailConfig},
        ...
   }
}
</pre>

crreEmailConfig peut prendre 3 valeurs en fonction des types suivants :
 - SMTP:
<pre>
{
   "name":"fr.openent~crre~1.1.8",
   "config":{
        ...
        "emailConfig": {
            "type": "SMTP",
            "hostname": ${crreSMTPHost},
            "port": ${crreSMTPPort},
            "username": ${crreSMTPUsername},
            "password": ${crreSMTPPassword},
            "split-recipients": ${crreSMTPSplitRecipients},
            "ssl": ${crreSMTPSSL},
            "tls": ${crreSMTPTLS}
        },
        ...
   }
}
</pre>
| **conf.properties**          | **Utilisation**                                          | **Exemple**                                |
|------------------------------|----------------------------------------------------------|--------------------------------------------|
| "${crreSMTPHost}"            | Smtp host                                                | yyy.ovh.net (String)                       |
| "${crreSMTPPort}"            | Smtp port                                                | 460 (Integer)                              |
| "${crreSMTPUsername}"        | Smtp username                                            | nepasrepondre.ovh@support-ovh.net (String) |
| "${crreSMTPPassword}"        | Smtp password                                            | MyStrongPassword (String)                  |
| "${crreSMTPSplitRecipients}" | Permet de crée un mail sépare pour chaques destinataires | true (Boolean)                             |
| "${crreSMTPSSL}"             | Active le SSL                                            | true (Boolean)                             |
| "${crreSMTPTLS}"             | Active le TLS                                            | true (Boolean)                             |

 - SendInBlue
<pre>
{
   "name":"fr.openent~crre~1.1.8",
   "config":{
        ...
        "emailConfig": {
            "type": "SendInBlue",
            "uri": ${crreSIBUri},
            "api-key": ${crreSIBApiKey},
            "ip": ${crreSIBIp},
            "max-size": ${crreSIBMaxSize},
            "date-pattern": ${crreSIBDatePattern}
        },
        ...
   }
}
</pre>
| **conf.properties**     | **Utilisation**                       | **Exemple**                        |
|-------------------------|---------------------------------------|------------------------------------|
| "${crreSIBUri}"         | SendInBlue uri                        | https://send.in.blue (String)      |
| "${crreSIBApiKey}"      | SendInBlue api key                    | SECUREAPIKEY (String)              |
| "${crreSIBIp}"          | SendInBlue API                        | 127.0.0.1 (String)                 |
| "${crreSIBMaxSize}"     | Taille maximum des mail               | 1000 (Integer)                     |
| "${crreSIBDatePattern}" | Format de date utilisé dans les mails | yyyy-MM-dd'T'HH:mm:ss.SSS (String) |

 - GoMail
<pre>
{
   "name":"fr.openent~crre~1.1.8",
   "config":{
        ...
        "emailConfig": {
            "type": "GoMail",
            "uri": ${crreGMUri},
            "user": ${crreGMUser},
            "password": ${crreGMPassword},
            "platform": ${crreGMPlatform}
        },
        ...
   }
}
</pre>
| **conf.properties** | **Utilisation** | **Exemple**               |
|---------------------|-----------------|---------------------------|
| "${crreGMUri}"      | GoMail uri      | https://go.mail (String)  |
| "${crreGMUser}"     | GoMail user     | myUser (String)           |
| "${crreGMPassword}" | GoMail password | MyStrongPassword (String) |
| "${crreGMPlatform}" | GoMail platform | customPlatform (String)   |

Si crreEmailConfig est null alors CRRE prend la configuration de base (Attention la variable ne doit pas etre un jsonObject vide mais bien null)