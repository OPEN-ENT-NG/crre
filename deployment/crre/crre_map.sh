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
        "equipment": {
            "properties": {
               "id": {
                    "type": "keyword"
                },
                "ean": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "name": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "summary": {
                    "type": "text"
                },
                "description": {
                    "type": "text"
                },
                "author": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "price": {
                    "type": "float"
                },
                "status": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "parution_date": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "option_enabled": {
                    "type": "boolean"
                },
                "reference": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "price_editable": {
                    "type": "boolean"
                },
                "offer": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "duration": {
                    "type": "integer"
                },
                "end_availability": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "tax_amount": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "editor_name": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "grade_name": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "subject_name": {
                    "type": "keyword",
                    "normalizer": "lower_normalizer"
                },
                "technical_specs": {
                    "type": "nested"
                }
            }
        }
    }
}
'

