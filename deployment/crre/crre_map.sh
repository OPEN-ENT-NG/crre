#!/bin/bash
curl -X PUT "http://localdev:9200/catalog" -H 'Content-Type: application/json' -d'
{
    "index": {
      "blocks": {
        "read_only_allow_delete": "false"
      }
    },
    "settings": {
      "analysis": {
        "normalizer": {
          "lower_normalizer": {
            "type": "custom",
            "char_filter": [],
            "filter": ["lowercase", "asciifolding"]
          }
        }
      }
    },
    "mappings": {
 "person": {
   "properties": {
     "name" : {
       "type": "keyword",
       "fields": {
         "keyword": {
           "type": "keyword"
         }
       }
      },
      "car": {
        "type" : "nested",
        "properties": {
          "make": {
            "type": "text",
            "fields": {
            "keyword": {
              "type": "keyword"
            }
           }
          },
          "model": {
            "type": "keyword",
            "fields": {
            "keyword": {
              "type": "keyword"
             }
            }
           }
         }
       }
     }
    }
   }
}
'

