# Serviço distribuído de backups

## Como compilar

### Linha de comandos
O projeto pode ser compilado usando o seguinte comando (na raíz do projeto):
1. rm -rf build && mkdir build
2. javac $(find -name "*.java") -d build
 
### Eclipse
Alternativamente pode ser usado o eclipse para abrir e compilar deste modo:
1. Abrir o eclipse.
2. No menu Files, selecionar a opção Import.
3. Escolher a pasta do projeto e confirmar.


## Como correr

### Peer

```java -Duser.dir=$(pwd) -cp build Peer.Peer <Peer_id> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT>```

ou 

```java -Duser.dir=$(pwd) -cp build Peer.Peer <Peer_id> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT> [capacity=1m]```

### Cliente
```java -Duser.dir=$(pwd) -cp build Client.Client <peer_ap> <operation> <opnd_1> <opnd_2>```


## Exemplo:
### Backup e Restore de um ficheiro .mp3 com 4 peers:
	java -Duser.dir=$(pwd) -cp build Peer.Peer 1 224.0.0.1 4046 224.0.0.2 4047 224.0.0.3 4048 1m
	java -Duser.dir=$(pwd) -cp build Peer.Peer 2 224.0.0.1 4046 224.0.0.2 4047 224.0.0.3 4048 1m
	java -Duser.dir=$(pwd) -cp build Peer.Peer 3 224.0.0.1 4046 224.0.0.2 4047 224.0.0.3 4048 1m
	java -Duser.dir=$(pwd) -cp build Peer.Peer 4 224.0.0.1 4046 224.0.0.2 4047 224.0.0.3 4048 1m

	java -Duser.dir=$(pwd) -cp build Client.Client 2 BACKUP music.mp3 2
	java -Duser.dir=$(pwd) -cp build Client.Client 2 RESTORE music.mp3



