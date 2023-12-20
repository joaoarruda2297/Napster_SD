

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;

public class Servidor {
	
	public static void main(String[] args) throws Exception
	{	
		Gson gson = new Gson();
		
		//criando um socket UDP
		DatagramSocket serverSocket = new DatagramSocket(10098);
		
		//cria o Hashmap que será usado para mapear os peers e locais dos arquivos
		Map<Integer,File> mapaPeers = new ConcurrentHashMap<Integer,File>();
		
		//cria um hashmap de peers que estão alive (ip e porta)
		Map<InetSocketAddress, Integer> peersAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		
		//cria um hash map de resposta de alive
		Map<InetSocketAddress, Integer> respAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		
		//cria um hash map de relacao portaAlive/portaTCP
		Map<Integer, Integer> peersPortas = new ConcurrentHashMap<Integer,Integer>();
		
		
		System.out.println("Servidor Napster no ar!");
		
		//roda thread de alive
		ThreadLoopAlive TdLA = new ThreadLoopAlive(peersAlive,serverSocket,peersPortas,respAlive,mapaPeers);
		TdLA.start();
		
		
		//aguarda conexão
		while(true)
		{
			//cria o pacote
			byte[] recebimento = new byte[1024];//bytes de classe mensagem
			DatagramPacket pacoteRecebimento = new DatagramPacket(recebimento, recebimento.length);
			
			//aguarda conexão
			serverSocket.receive(pacoteRecebimento);//blocking
			String mensagem = new String(pacoteRecebimento.getData(),pacoteRecebimento.getOffset(),pacoteRecebimento.getLength());//transforma a requisicao em string
			Mensagem msg = gson.fromJson(mensagem,Mensagem.class);
			
			//chama a thread pra mandar o menu
			ThreadRequisicao TdReq = new ThreadRequisicao(mapaPeers,msg,serverSocket,pacoteRecebimento,peersAlive, respAlive, peersPortas);
			TdReq.start();
		}
	}
	
	public static void recebeDownload(Map<Integer, File> mapaPeers, DatagramSocket serverSocket, Mensagem msg, InetAddress ipnovo, int portanova) 
	{
		
		try 
		{	
			Gson gson = new Gson();
			
			int achei=0;
			
			//lista para guardar os peers que possuem o arquivo
			ArrayList<String> peersQuePossuem = new ArrayList<>();
			
			//cria o array na mensagem pra caso nao tenha rodado o search
			for (int porta : mapaPeers.keySet()) 
			{
                //Capturamos o valor a partir da chave
                File localArquivos = mapaPeers.get(porta);
                //listamos os arquivos em um vetor de string
                String[] nomeArquivos = localArquivos.list();
                
                //percorre a lista de arquivos dentro da pasta
                for(int i=0; i<nomeArquivos.length;i++)
                {
                	if(nomeArquivos[i].equals(msg.getElementoBusca()))
                	{
                		String door  = Integer.toString(porta);
                		peersQuePossuem.add(door);
                	}
                }
			}
			
			//inicia a busca pelo arquivo no peer e porta definidos pela mensagem
			for (int porta : mapaPeers.keySet()) 
			{
				//encontra o peer dessa porta
                if(porta == msg.getPortaBusca())
                {
                	//cria uma lista com o nome dos locais
                	File localArquivos = mapaPeers.get(porta);
                    //listamos os arquivos em um vetor de string
                    String[] nomeArquivos = localArquivos.list();
                    
                    for(int i=0; i<nomeArquivos.length;i++)
                    {
                    	if(nomeArquivos[i].equals(msg.getElementoBusca()))
                    	{
                    		achei = 1;
                    	}
                    }
                }
			}
			
			//se encontrou envia download ok, se nao envia download not ok
			if(achei==1)
			{
				msg.setTipo("DOWNLOAD_OK");
				
			}	 
			else msg.setTipo("DOWNLOAD_NEGADO");
			
			//envia por UDP de volta ao cliente
			byte[] envio = gson.toJson(msg).getBytes();
			DatagramPacket envioDown = new DatagramPacket(envio, envio.length, ipnovo, portanova);
			serverSocket.send(envioDown);
			
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void recebeSearch(Map<Integer, File> mapaPeers, DatagramSocket serverSocket, Mensagem msg) 
	{
		try {
			Gson gson = new Gson();
			
			//printa a solicitação ao servidor
			System.out.println("Peer"+ msg.getIp() +":"+msg.getPortaTCP()+" solicitou arquivo "+ msg.getElementoBusca());
			
			//lista para guardar os peers que possuem o arquivo
			ArrayList<String> peersQuePossuem = new ArrayList<>();
			
			for (int porta : mapaPeers.keySet()) 
			{
                //Capturamos o valor a partir da chave
                File localArquivos = mapaPeers.get(porta);
                //listamos os arquivos em um vetor de string
                String[] nomeArquivos = localArquivos.list();
                
                //percorre a lista de arquivos dentro da pasta
                for(int i=0; i<nomeArquivos.length;i++)
                {
                	if(nomeArquivos[i].equals(msg.getElementoBusca()))
                	{
                		String door  = Integer.toString(porta);
                		peersQuePossuem.add(door);
                	}
                }
			}
			
			//guarda no objeto mensagem o array de peers
			msg.setArquivos(peersQuePossuem);
			if(peersQuePossuem.size()>0) msg.setTipo("SEARCH_OK");
			else msg.setTipo("SEARCH_NOT_OK");
			
			//envia a mensagem ao cliente
			byte[] envio = gson.toJson(msg).getBytes();
			DatagramPacket envioSearch = new DatagramPacket(envio, envio.length, msg.getIp(), msg.getPorta());
			serverSocket.send(envioSearch);
			
			
			
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		
	}

	private static void recebeJoin(Map<Integer,File> mapaPeers, DatagramSocket serverSocket, Mensagem msg, Map<InetSocketAddress,Integer> peersAlive, Map<Integer,Integer> peersPortas)
	{
		try
		{
			Gson gson = new Gson();

			File localArquivos = new File(msg.getLocal());//obtem o nome do local
			String[] nomeArquivos = localArquivos.list();//lista os nomes dos arquivos
			
			//coloca dentro do HashMap a porta TCP e o local
			mapaPeers.put(msg.getPortaTCP(), localArquivos);
			
			//coloca dentro do portaspeers
			peersPortas.put(msg.getPortaAlive(), msg.getPortaTCP());
			
			//coloca dentro do hashmap ip e porta do peer alive
			InetSocketAddress IPPorta = new InetSocketAddress(msg.getIp(),msg.getPortaAlive());
			peersAlive.put(IPPorta, msg.getPortaAlive());
			
			//printa a resposta ao servidor
			System.out.print("Peer" + msg.getIp() + ":" + msg.getPortaTCP() + " adicionado com os arquivos ");
			for(int i=0;i<nomeArquivos.length;i++)
			{
				System.out.print(nomeArquivos[i]+ " ");
			}
			System.out.println("");
			
			//altera o tipo da mensagem
			msg.setTipo("JOIN_OK");
			
			//envia o JOIN_OK
			byte[] envioJoin = gson.toJson(msg).getBytes();
			DatagramPacket envioJoinOK = new DatagramPacket(envioJoin, envioJoin.length, msg.getIp(), msg.getPorta());
			serverSocket.send(envioJoinOK);
			
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void recebeLeave(Map<Integer,File> mapaPeers, DatagramSocket serverSocket, Mensagem msg, Map<InetSocketAddress, Integer> peersAlive, Map<Integer, Integer> peersPortas)
	{
		try {
			Gson gson = new Gson();
			
			//remove o indice do hashmap com base na porta de acesso
			mapaPeers.remove(msg.getPortaTCP());
			
			//tira do peersAlive e do peersPortas
			InetSocketAddress ipporta = new InetSocketAddress(msg.getIp(),msg.getPortaAlive());
			peersAlive.remove(ipporta);
			peersPortas.remove(msg.getPortaAlive());
			
			//altera o tipo da mensagem
			msg.setTipo("LEAVE_OK");
			
			//envia o LEAVE_OK
			if(msg.getPorta() != 0)
			{
				byte[] envio = gson.toJson(msg).getBytes();
				DatagramPacket envioLeave = new DatagramPacket(envio, envio.length, msg.getIp(), msg.getPorta());
				serverSocket.send(envioLeave);
			}
			
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}
	
	public static class ThreadRequisicao extends Thread
	{
		Map<Integer,File> mapaPeers = new ConcurrentHashMap<Integer,File>();
		Map<InetSocketAddress,Integer> peersAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		Map<InetSocketAddress,Integer> respAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		Map<Integer,Integer> peersPortas = new ConcurrentHashMap<Integer,Integer>();
		Mensagem msg;
		DatagramSocket serverSocket;
		DatagramPacket pacoteRecebimento;
		
		//construtor
		public ThreadRequisicao(Map<Integer, File> HashPeers, Mensagem message, DatagramSocket serverSoquete, DatagramPacket pacote, Map<InetSocketAddress,Integer> peersAliv, Map<InetSocketAddress,Integer> rAlive, Map<Integer,Integer> peersPorta)
		{
			mapaPeers = HashPeers;
			msg = message;
			serverSocket = serverSoquete;
			pacoteRecebimento = pacote;
			peersAlive = peersAliv;
			respAlive = rAlive;
			peersPortas = peersPorta;
		}
		
		public void run()
		{	
			try 
			{		
				//roda o menu
				if(msg.getTipo().equals("JOIN"))
				{
					recebeJoin(mapaPeers, serverSocket, msg, peersAlive, peersPortas);
				}
				else if(msg.getTipo().equals("SEARCH"))
				{
					recebeSearch(mapaPeers, serverSocket, msg);
				}
				else if(msg.getTipo().equals("DOWNLOAD"))
				{	
					InetAddress ipnovo = pacoteRecebimento.getAddress();
					int portanova = pacoteRecebimento.getPort();
					
					recebeDownload(mapaPeers, serverSocket, msg, ipnovo, portanova);
				}
				else if(msg.getTipo().equals("LEAVE"))
				{
					recebeLeave(mapaPeers, serverSocket, msg, peersAlive, peersPortas);
				}
				else if(msg.getTipo().equals("ALIVE_OK"))
				{
					InetSocketAddress ipporta = new InetSocketAddress(pacoteRecebimento.getAddress(), pacoteRecebimento.getPort());
					respAlive.put(ipporta, pacoteRecebimento.getPort());
				}
				
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			
		}
	}
	
	public static class ThreadLoopAlive extends Thread
	{
		Map<InetSocketAddress,Integer> peersAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		Map<InetSocketAddress,Integer> respAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		Map<Integer,Integer> peersPortas = new ConcurrentHashMap<Integer,Integer>();
		Map<Integer,File> mapaPeers = new ConcurrentHashMap<Integer,File>();
		
		DatagramSocket serverSocket;
		
		public ThreadLoopAlive(Map<InetSocketAddress,Integer> peersAliv, DatagramSocket serverSoquete,Map<Integer,Integer> peersPorta, Map<InetSocketAddress,Integer> respAliv,Map<Integer,File> mapaPeer)
		{
			peersAlive = peersAliv;
			serverSocket = serverSoquete;
			peersPortas = peersPorta;
			respAlive = respAliv;
			mapaPeers = mapaPeer;
		}
		
		public void run()
		{
			try 
			{
				
				while(true)
				{	
					//espera 30s pra rodar
					sleep(30000);
					
					//limpa o respalive
					respAlive.clear();
					
					//manda para thread de verificacao
					ThreadVefAlive TdVA = new ThreadVefAlive(peersAlive,serverSocket,respAlive,peersPortas,mapaPeers);
					TdVA.start();
					
				}
				
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		}
	}

	public static class ThreadVefAlive extends Thread
	{
		Map<InetSocketAddress,Integer> peersAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		Map<Integer,Integer> peersPortas = new ConcurrentHashMap<Integer,Integer>();
		DatagramSocket serverSocket;
		Map<InetSocketAddress,Integer> respAlive = new ConcurrentHashMap<InetSocketAddress,Integer>();
		Map<Integer,File> mapaPeers = new ConcurrentHashMap<Integer,File>();
		
		public ThreadVefAlive(Map<InetSocketAddress,Integer> peersAliv, DatagramSocket serverSocket2, Map<InetSocketAddress,Integer> respAliv, Map<Integer,Integer> peersPorta,Map<Integer,File> mapaPeer)
		{
			peersAlive = peersAliv;
			serverSocket = serverSocket2;
			respAlive = respAliv;
			peersPortas = peersPorta;
			mapaPeers = mapaPeer;
		}
		
		
		
		public void run()
		{
			try 
			{
				//rodo o hash de peers e em cada um eu mando uma mensage udp, se nao receber de volta o peer morreu
				for(InetSocketAddress ipporta : peersAlive.keySet())
				{
					
					int porta = peersAlive.get(ipporta);//problema
					//thread de envio pra cada porta
					ThreadEnvioAlive TdEA = new ThreadEnvioAlive(serverSocket, porta, ipporta.getAddress(), peersPortas);
					TdEA.start();
				}
				
				sleep(3000);
				
				//comparação entre peersAlive (que estavam vivos anteriormente)
				//e o respAlive(que responderam por sua vida na ultima requisicao de ali
				for(InetSocketAddress ip2porta : peersAlive.keySet())
				{
					int achei = 0;
					int porta2 = peersAlive.get(ip2porta);//problema
					
					for(InetSocketAddress ipFporta : respAlive.keySet())
					{
						int portaF = respAlive.get(ipFporta);
						
						if(portaF == porta2)
						{
							achei = 1;
						}
					}
					
					if(achei == 0)
					{
						int X = peersPortas.get(porta2);
						
						Mensagem msg = new Mensagem(X);
						
						File localArquivos = mapaPeers.get(X);
						String[] arquivos = localArquivos.list();
						
						System.out.println("Peer "+ ip2porta.getAddress() + ":"+ msg.getPortaTCP() +" morto. Eliminando seus arquivos ");
						for(int j=0;j<arquivos.length;j++)
						{
							System.out.print(arquivos[j] + " ");
						}
						msg.setIp(ip2porta.getAddress());
						msg.setPortaAlive(porta2);
						recebeLeave(mapaPeers, serverSocket, msg, peersAlive, peersPortas);
						
						
						
						
					}
					
					
					
				}
				
				
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	public static class ThreadEnvioAlive extends Thread
	{
		DatagramSocket serverSocket;
		InetAddress ip;
		int porta;
		Map<Integer,Integer> peersPortas = new ConcurrentHashMap<Integer,Integer>();
		
		public ThreadEnvioAlive(DatagramSocket serverSoquete, int port, InetAddress IP, Map<Integer,Integer> peersPorta)
		{
			serverSocket = serverSoquete;
			porta = port;
			ip = IP;
			peersPortas = peersPorta;
		}
		
		public void run()
		{
			try 
			{
				Gson gson = new Gson();
				
				Mensagem msg = new Mensagem("ALIVE?");
				
				//envio pra porta UDP do cliente
				byte[] envioAlive = gson.toJson(msg).getBytes();
				DatagramPacket envioAliv = new DatagramPacket(envioAlive, envioAlive.length, ip, porta);
				serverSocket.send(envioAliv);

				
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
}
