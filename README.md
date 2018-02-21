# Avvio del sistema

## Docker Compose
Permette di avviare contemporaneamente più container, tra cui le repliche di MariaDB (solo usa per adesso) e RabbitMQ.

Per avviare docker-compose:

- Installare [docker](https://docs.docker.com/install/linux/docker-ce/ubuntu/) e [docker-compose](https://docs.docker.com/compose/install/).
- Posizionarsi nella cartella /docker e dare `docker-compose up` per avviare i container.
------
Al primo avvio dei container, è necessario creare la tabella LOG in ognuna delle repliche. 
Per fare ciò, è necessario dare il seguente comando `mysql -h 127.0.0.1 -P NUMPORTA -u root -proot` 
e creare la tabella utilizzando i seguenti comandi:
```
> use ELENCO;
> create TABLE LOG(timestamp TIMESTAMP, idMacchina VARCHAR(255), message VARCHAR(255));
```
con NUMPORTA diverso di volta in volta per tutte le repliche (3306-3310).

## Netbeans Project
Per avviare il progetto, è necessario aprire i progetti QuorumProject e Quorum_Project_utilities
con il software Netbeans, buildarli entrambi, e infine eseguire il deploy e il run del primo.

## Analyzer
Per avviare l'analyzer, il quale è un'applicazione sviluppata con le tecnologie Angular 2 e Node.js,
bisogna posizionarsi nella cartella /Analyzer/my-app e dare i seguenti comandi:

* Scaricare la docker image: `docker pull trion/ng-cli`
* Buildare l'app: `docker run -u $(id -u) --rm -v "$PWD":/app trion/ng-cli ng build`
* Runnare l'analyzer con: `docker run -u $(id -u) --rm -p 4200:4200 -v "$PWD":/app trion/ng-cli ng serve -host 0.0.0.0`

(*Reference: https://hub.docker.com/r/trion/ng-cli/*)

## Message Handler e Scanner
L'ordine in cui devono essere avviati questi due componenti è il seguente:
Per la versione Publish/Subscribe:

1. StartHandlers.java
2. Scanner.java

Per la versione Work queues:

1. WorkerMSGHandler.java
2. WorkerScanner.java

## Client

Il progetto Quorum_Project_client è stato utilizzato per effettuare test. Non è quindi necessario avviarlo.
