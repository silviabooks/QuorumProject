# Progetto Sistemi Distribuiti

Elaborato di corso per Progettazione di Sistemi Distribuiti, a.a. 2017/2018

# Architettura

Il sistema è stato realizzato utilizzando come pattern di riferimento il Model View Controller. 
Qui di seguito si elencano i layer di cui è composto il sistema e la loro implementazione interna:

- **Presentation Layer**: La parte di Presentation viene gestita da un EJB che utilizza la REST API (con metodo GET) per poter eseguire la read.
- **Business Layer**: La Business logic è implementata all’interno del data manager. Ogni replica possiede un EJB destinato alla sua gestione. Il protocollo utilizzato per la consistenza delle repliche è basato su quorum. Inoltre, è presente un EJB che si occupa della gestione dei fault basato su timeout.
- **Data Layer**: Questo layer contiene le diverse repliche. Le varie repliche saranno avviate con più container Docker (da decidere ancora se utilizzare MySQL oppure MongoDB).

Addizionali a questa struttura sono gli scanner, la coda e i MSG Handler.
Gli scanner sono implementati come classi Java (oppure script Python) che si occupa di 
leggere le righe di un file log e filtrarle per parola chiave/RegEx. Fatto ciò, gli scanner si comportano 
da publisher, inviando il dato letto ad una coda. La coda è implementata con RabbitMQ. I MSG Handler sono 
implementati come delle classi Java (oppure script Python) che consumano il contenuto della coda e, in modo 
content-based, invieranno i dati al Data Manager. L’invio avviene mediante REST API (con metodo POST).

L’**Analyzer** si occuperà di eseguire particolari query al Data Manager. 
Per gestire la sua interazione con il DM viene utilizzato un EJB che utilizza sempre una interfaccia REST.

Il **Data Manager** è il fulcro della gestione della consistenza delle repliche. Presenta due EJB singleton che si occuperanno, rispettivamente, della gestione della consistenza delle repliche e della gestione nel caso in cui una replica sperimenti un crash. La comunicazione tra DM e l’esterno è mediata da due EJB che implementano le REST API di Read e Write.
L’EJB che si occupa della consistenza è un EJB Singleton (oppure Stateless) che funge da Proxy. Questo riceve le richieste dai MSG Handler e dall’Analyzer e si occupa di raccogliere il quorum:

- Read quorum: Settato a Qr = 2
- Write quorum: Settato a Qw = 4

## Read

1. Il proxy contatta Qr repliche
2. Ottenuto il dato interessato e gli specifici Version Number, confronta i due valori ottenuti
3. Il proxy fornisce all’analyzer il dato che ha fornito la replica con Version Number più elevato

## Write

1. Il proxy, alla ricezione di una richiesta di write da parte dei MSG Handelr, contatta i RM per raccogliere il quorum, richiedendo il loro Version Number ed inviando il dato da scrivere
2. I RM conservano il dato nella loro log queue e lo marcano come non disponibile. In seguito, inviano il loro Version Number al proxy
3. Il proxy riceve i diversi Version Number:
	- Se rispondono almeno 4 repliche, la scrittura può avvenire. il proxy confronta i Version Number e invia ai RM il Version Number più alto. N.B: se una delle repliche che rispondono al proxy non è allineata (cioè ha un VN minore delle altre) non è necessario che lo sia prima che avvenga la scrittura (una possibile aggiunta potrebbe essere l’implementazione del Write Back dei valori dalle altre repliche).
	- Se rispondono meno di 4 repliche, il quorum di scrittura non è raggiunto e la scrittura non va a buon fine. In tal caso, si può comunicare al client che la scrittura non è avvenuta e può riprovare dopo un certo periodo di tempo, per un numero limitato di tentativi. In tal caso, il proxy avvisa le repliche che hanno risposto di scartare la scrittura presente in log queue.
4. I Replica Manager aggiornano il loro Version Number basandosi su quello inviato dal proxy e riordinano la loro coda in base a quest’ultimo, marcando la scrittura come disponibile. Se l’elemento in testa alla coda è disponibile, la RM esegue la scrittura del dato nel database. Questo procedimento continua fino a quando la testa della coda ha valori marcati come disponibili

I Version Number utilizzati sono costituiti da una coppia <sq, ID>:

- sq è il valore dello unix timestamp in quella replica
- ID è l’identificativo della replica

Tale gestione permette scritture in Total Ordering. 
E’ stata scartata l’idea di un Sequencer per stabilire i VN perché risulterebbe un collo di bottiglia nel caso in cui il sistema sia soggetto ad uno Scale In o Scale Out.

## Fault detection

L’EJB che si occupa dei fault è un Singleton (oppure Stateless) che implementa un fault detector basato su timeout. 
Esso riceve costantemente dalle RM un messaggio di Heartbeat, che segnala l’attività della replica. 
Nel momento in cui una replica non risponde entro un certo intervallo di tempo, l’EJB passerà ad effettuare
un controllo più specifico, eseguendo un ping-ack sulla specifica replica. Tale ping-ack impone un timeout più elevato. Se entro questo periodo la replica non risponde, essa è considerata in crash: il fault detector informerà il proxy di non contattare più quella replica. Il fault detector avviserà che una replica è caduta e che è necessaria una riconfigurazione del quorum. Il sistema tollera il crash al più di una replica. A seguito del crash di più di una replica, la scrittura viene bloccata e si potrà eseguire solo la lettura. A seguito del crash di 4 o più repliche, il sistema non sarà in grado di effettuare le letture.
Non è previsto alcun controllo per il recupero da crash di una replica.
Le repliche sono fisse e l’eventuale aggiunta di un nuovo elemento non è previsto a caldo.
