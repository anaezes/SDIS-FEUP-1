# Introdução

Este projeto é para desenvolver um serviço distribuído de backups.

*Primeira Fase
	- Criar classe "Peer".
	- Ligar aos diferentes canais.
	- Testar os "Peer's" : Escrever e ler strings em cada um dos canais.

java.lang.Thread
	extends Thread
java.lang.Runnable
	implements Runnable

java.util.concurrent ThreadPoolExecutor
	execute(Runnable r)

java.util.concurrent tem várias classes de estruturas de dados Thread-safe (Filas, HashMaps...)


________________________________________________________________________________________________________________________



Tutorial Getting Started Using Java™ RMI - https://docs.oracle.com/javase/7/docs/technotes/guides/rmi/hello/hello-world.html
1 - Interface remota
	interface ControlInterface extends Remote {
		int backup ( , , , ) throws RemoteException
	}

2 - Implementar a interface
	class Control implements ControllerInterface {
		public Control() {}
		
		int backup( , , , ) {
			...
		}
	}

3 - Setup de objeto remoto
	Control control = new Control ( , );
	ControlInterface proxy = java.rmi.server.UnicastRemoteObject.exportObject(control)

java.rmi.registry.Registry reg = LocateRegistry.getRegistry();
reg.(re)bind("name", proxy)

4 - 
	ControlInterface proxy;
	Registry reg = LocateRegistry.getRegistry() ;
	proxy = reg.Lookup("name");
	proxy.backup( , , , )

Idealmente criar um rmiRegistry por peer, mas tem complicações de ports (default 1099)

	
________________________________________________________________________________________________________________________

TCP in Java - Restore protocol enhancement

Asymetric Model
ServerSocket 
	usado no lado do servidor
	exclusivamente para estabelecer conecções

Socket
	usados tanto pelo servidor como pelo cliente
	usado para transferir dados


ServerSocket(int port)
ServerSocket(int port, int backlog, InetAddress addr)
	Socket	ServerSocket.accept();
	ServerSocket.close();

Socket(InetAddr addr, int port)
	usar java.io. para transferir dados (streams)
	InputStream Socket.getInputStream(); // Ler dados -> InputStreamReader -> BufferedReader
	OutputStream Socker.getOutputStream(); // Enviar dados -> PrintWriter
	Socket.close(); // Não deve de ser usado pois pode causar perda de dados
	Socket.shutdownOutput(); // Maneira correta de encerrar a ligação. O emissor de dados envia um pedido para fechar o receptor

PrintWriter(OutputStream)
	PrintWriter.printLn();
	Esta stream tenta acumular bytes para fazer a escrita toda ao mesmo tempo para o disto. Pode ser necessário para usar flush

InputStreamReader(InputStream)

BufferedReader(InputStreamReader)
	BufferedReader.ReadLine();

Streams capazes de enviar objetos inteiros que implementem a interface Serializable
	ObjectInputStream
	ObjectOutputStream

________________________________________________________________________________________________________________________