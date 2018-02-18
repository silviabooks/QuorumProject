#API
* Eventi ordinati nel tempo per tutte le macchine: `http://localhost:8080/QuorumProject-war/gestione/log/get`
* Eventi ordinati nel tempo per una singola macchina: `http://localhost:8080/QuorumProject-war/gestione/log/get/{idMacchina}`
* Eventi in una certa finestra temporale (timestamp nel formato `2018-02-18 16:43:03`): `http://localhost:8080/QuorumProject-war/gestione/log/timestamp/{timestampBegin}/{timestampEnd}`
