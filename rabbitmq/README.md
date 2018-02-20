# Scanner e Message Handler

## RabbitMQ
RabbitMQ è un message broker open-source, che implementa un sistema a code per
l'invio e la ricezione dei messaggi. 
*prendere robe dal quaderno*
In questo, il producer è lo Scanner, e il consumer è il Message Handler.

Sono disponibili due implementazioni dei due elementi del sistema:

* *Work queues*: permette di eseguire un task time-comsuming da parte del consumer. Infatti, il consumer viene implementato come un worker che esegue in background e consuma gli elementi dalla coda attraverso dei task. 
* *Publish/subscribe*: consente la presenza di più consumer, ognuno dei quali consuma una riga di log con un IP diverso (i due IP specificati nell'homework). In questo caso l'exchange type è `fanout`: esso permette di avere più consumatori che non vanno in conflitto tra loro.

## Scanner
Lo scanner è stato implementato come una classe Java che va a leggere il file
in cui sono contenuti i log riga per riga. Ogni volta che c’è un entry WARN, 
l’intera riga di log viene pubblicata nella coda. 
RabbitMQ mette a disposizione una ConnectionFactory per la creazione della 
connessione tra lo Scanner e RabbitMQ in esecuzione sulla macchina alla porta di 
default 5672. Viene quindi aperto un canale, impostato l'exchange type e inviate
tutte le linee di log con la funzione basicPublish. 
Quando lo Scanner non ha più niente da leggere, vengono chiusi il canale e la 
connessione.
L'implementazione dello Scanner è stata eseguita con una semplice classe Java con
un main.


## Message Handler
Il Message Handler consuma gli elementi della coda e li analizza 
(Content-based mode).
Per fare ciò, vengono utilizzate delle Regular expression per individuare i 
timestamp, gli ID macchina e i messaggi. Se la riga di log che viene prelevata 
dalla coda contiene i pattern specificati dalle regEx, viene salvata su DB 
effettuando la richesta HTTP POST all'indirizzo specifico delle API.



