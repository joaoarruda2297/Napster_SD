

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import com.google.gson.Gson;


public class Peer {
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("Olá! Cliente P2P no ar!");
		Scanner scan = new Scanner(System.in);
		
		//local
		String local = "";
		
		//ip
		System.out.println("Digite seu IP:");
		String IP = scan.next();
		InetAddress ip = InetAddress.getByName(IP);
		
		//porta UDP requisicoes gerais
		System.out.println("Digite sua porta:");
		int porta = scan.nextInt();
		
		//definindo o socket UDP
		DatagramSocket clienteSocket = new DatagramSocket(porta);
		
		//definindo socket udp só pro alive
		DatagramSocket aliveSocket = new DatagramSocket(0);
		
		//montando o socketTCP (funciona como de um servidor)
		ServerSocket serverTCPSocket = new ServerSocket(0);
		
		//obtem a porta TCP
		int portaTCP = serverTCPSocket.getLocalPort();
		
		//construindo a mensagem
		Mensagem msg = new Mensagem(ip,porta,portaTCP,aliveSocket.getLocalPort());
		
		int saiu = 0;
		
		//rodando o menu
		while(saiu==0)
		{
			System.out.println("Digite o número de sua escolha:");
			System.out.println("1-Join\n"
			         		 + "2-Search\n"
			         		 + "3-Download\n"
			         		 + "4-Leave");
			
			int escolha = scan.nextInt();
			
			if(escolha == 1)//join
			{	
				//local
				System.out.println("Digite o local do arquivo:");
				local = scan.next();
				
				//montando o objeto mensagem
				msg.setLocal(local);
				msg.setTipo("JOIN");
				
				//chamando a funcao JOIN
				msg = funcaoJoin(clienteSocket,msg);
				
				//thread pra fazer o loop do download
				ThreadLoopDown TdLoop = new ThreadLoopDown(serverTCPSocket, clienteSocket,local);
				TdLoop.start();
				
				//thread para responder se o peer esta alive
				ThreadClienteLoopAlive TdCLA = new ThreadClienteLoopAlive(aliveSocket);
				TdCLA.start();
				
				
			}
			
			else if(escolha == 2)//search
			{
				System.out.println("Qual é o nome do arquivo que deseja?");
				String elementoBusca = scan.next();
				
				//montando a mensagem
				msg.setElementoBusca(elementoBusca);
				msg.setTipo("SEARCH");
				
				funcaoSearch(clienteSocket, elementoBusca, msg);
			}
			
			else if(escolha == 3)//download
			{
				System.out.println("Digite o IP do peer que quer pedir o download:");
				String downIP = scan.next();
				
				System.out.println("Digite a porta do peer que quer pedir o download:");
				int downPorta = scan.nextInt();
				
				System.out.println("Digite o nome do arquivo que quer fazer download:");
				String downArquivo = scan.next();
				
				//chama o download por THREAD
				ThreadReqDownload TdReqDown = new ThreadReqDownload(msg,downIP,downPorta,downArquivo);
				TdReqDown.start();
			}
			
			else if(escolha == 4)//leave
			{
				//montando a mensagem
				msg.setTipo("LEAVE");
				
				funcaoLeave(clienteSocket,msg);
				saiu = 1;
			}
		}
		
		scan.close();
		serverTCPSocket.close();
		clienteSocket.close();
		System.exit(0);
	}

	
	public static void funcaoDownload(Mensagem msg, String downIP, int downPorta, String downArquivo)
	{	
		try 
		{
			Gson gson = new Gson();
			
			//cria um socket pra se conectar com outro peer via tcp
			Socket sDown = new Socket(downIP, downPorta);
			
			//arruma a mensagem para ser enviada
			msg.setElementoBusca(downArquivo);
			InetAddress IP = InetAddress.getByName(downIP);
			msg.setIpBusca(IP);
			msg.setPortaBusca(downPorta);
			
			String msgEnvio = gson.toJson(msg);
			
			//cria o meio de envio
			OutputStream os = sDown.getOutputStream();
			DataOutputStream writer = new DataOutputStream(os);
			
			//envia
			writer.writeBytes(msgEnvio + "\n");
			
			
			File localArquivo = new File(msg.getLocal() + "\\" + downArquivo);//ok
			FileOutputStream fileOutput = new FileOutputStream(localArquivo);//ok
			BufferedOutputStream buffOutput = new BufferedOutputStream(fileOutput);
			InputStream iStream = sDown.getInputStream();//ok
			
			byte data[] = new byte[4096];
			
			int count;
			count = iStream.read(data);
			
			while(count != -1)
			{
				buffOutput.write(data,0,count);
				count = iStream.read(data);
			}
			
			System.out.println("Arquivo "+ downArquivo + " baixado com sucesso na pasta " + msg.getLocal());
			
			iStream.close();
			sDown.close();
			buffOutput.close();
			
		}
		
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		
	}
	
	private static void funcaoSearch(DatagramSocket clienteSocket, String elementoBusca, Mensagem msg) throws Exception
	{
		Gson gson = new Gson();
		
		//enviando a mensagem SEARCH para o servidor
		byte[] msgEnvio = gson.toJson(msg).getBytes();
		DatagramPacket envioSearch = new DatagramPacket(msgEnvio, msgEnvio.length, msg.getIp(), msg.getPortaServer());
		clienteSocket.send(envioSearch);
		
		//aguardando mensagem SEARCH do servidor
		byte[] recebimento = new byte[1024];
		DatagramPacket recebeSearch = new DatagramPacket(recebimento, recebimento.length);
		clienteSocket.receive(recebeSearch);
		String mensagem = new String(recebeSearch.getData(),recebeSearch.getOffset(),recebeSearch.getLength());//transforma a requisicao em string
		msg = gson.fromJson(mensagem,Mensagem.class);
		
		//printando
		System.out.println("Peers com o arquivo solicitado:");
		for(int i=0;i<msg.getArquivos().size();i++)
		{
			System.out.println("127.0.0.1:"+ msg.getArquivos().get(i));
		}
	}

	private static Mensagem funcaoJoin(DatagramSocket clienteSocket, Mensagem msg) throws Exception
	{	
		Gson gson = new Gson();

		//local
		File localArquivos = new File(msg.getLocal()); //transforma em file
		String[] arquivos = localArquivos.list(); //lista em um vetor
		ArrayList<String> ARQUIVOS = devolveArquivos(arquivos); //funcao transforma
		msg.setArquivos(ARQUIVOS);
		
		//enviando JOIN ao servidor pela mensagem
		byte[] msgEnvio = gson.toJson(msg).getBytes();
		DatagramPacket envioJoin = new DatagramPacket(msgEnvio, msgEnvio.length, msg.getIp(), msg.getPortaServer());
		clienteSocket.send(envioJoin);
		
		//aguardando o JOIN_OK do servidor
		byte[] recebimento = new byte[1024];
		DatagramPacket recebeJoin = new DatagramPacket(recebimento, recebimento.length);
		
		while(msg.getTipo().equals("JOIN"))
		{
			clienteSocket.receive(recebeJoin);
			String mensagem = new String(recebeJoin.getData(),recebeJoin.getOffset(),recebeJoin.getLength());//transforma a requisicao em string
			msg = gson.fromJson(mensagem,Mensagem.class);
			
		}
		
		
		System.out.print("Sou peer " + msg.getIp() + ":" + msg.getPortaTCP() + " ");
		for(int i=0;i<msg.getArquivos().size();i++)
		{
			System.out.print(msg.getArquivos().get(i) + " ");
		}
		System.out.println("");
		
		return msg;
	}
	
	private static ArrayList<String> devolveArquivos(String[] arquivos)
	{
		ArrayList<String> ARQUIVOS = new ArrayList<>();
		
		for(int i=0;i<arquivos.length;i++)
		{
			ARQUIVOS.add(arquivos[i]);
		}
		return ARQUIVOS;
	}
	
	private static void funcaoLeave(DatagramSocket clienteSocket, Mensagem msg) throws Exception
	{
		Gson gson = new Gson();

		//enviando a requisicao leave ao servidor
		byte[] msgEnvio = gson.toJson(msg).getBytes();
		DatagramPacket envioLeave = new DatagramPacket(msgEnvio, msgEnvio.length, msg.getIp(), msg.getPortaServer());
		clienteSocket.send(envioLeave);
		
		//aguarda a resposta LEAVE_OK
		byte[] recebimento = new byte[1024];
		DatagramPacket recebeLeave = new DatagramPacket(recebimento, recebimento.length);
		
		while(msg.getTipo().equals("LEAVE"))
		{
			clienteSocket.receive(recebeLeave);
			String mensagem = new String(recebeLeave.getData(),recebeLeave.getOffset(),recebeLeave.getLength());//transforma a requisicao em string
			msg = gson.fromJson(mensagem,Mensagem.class);
		}
	}

	public static class ThreadDownload extends Thread
	{
		Socket no;
		DatagramSocket clienteSocket;
		String local;
		
		//construtor
		public ThreadDownload(Socket NO, DatagramSocket clienteSoquete, String localidade)
		{
			no = NO;
			clienteSocket = clienteSoquete;
			local = localidade;
		}
		
		public void run()
		{
			try 
			{
				Gson gson = new Gson();
				
				String flag = "DOWNLOAD_NEGADO";
				
				//array de peers ja buscados
				ArrayList<String> buscarPeer;
				
				//cria o leitor que vai receber a mensagem de outro peer
				InputStreamReader is = new InputStreamReader(no.getInputStream());
				BufferedReader reader = new BufferedReader(is);
				
				//recebe de outro peer
				String mensagem = reader.readLine();
				Mensagem msg = gson.fromJson(mensagem, Mensagem.class);
				
				//a política para receber o download negado é para caso o cliente buscou por um arquivo, encontrou ele vindo de um peer,
				//mas ao tentar se conectar com ele, o servidor verificou se ele ainda está na rede.
				//ele poderia nao estar mais por uma saída brusca que depois foi verificada pelo alive
				
				while(flag.equals("DOWNLOAD_NEGADO"))
				{
					//verifica com o servidor se o arquivo existe no peer selecionado via UDP
					msg.setTipo("DOWNLOAD");
					byte[] msgEnvio = gson.toJson(msg).getBytes();
					DatagramPacket envioDownload = new DatagramPacket(msgEnvio, msgEnvio.length, msg.getIp(), msg.getPortaServer());
					clienteSocket.send(envioDownload);
					
					//aguarda do servidor se o arquivo dentro do peer foi encontrado
					byte[] recebimento = new byte[1024];
					DatagramPacket recebeDown = new DatagramPacket(recebimento, recebimento.length);
					clienteSocket.receive(recebeDown);
					String mensagemUDP = new String(recebeDown.getData(),recebeDown.getOffset(),recebeDown.getLength());//transforma a requisicao em string
					msg = gson.fromJson(mensagemUDP,Mensagem.class);
					
					//guarda os peers que possuem no array de buscar
					buscarPeer = msg.getArquivos();
					
					if(msg.getTipo().equals("DOWNLOAD_OK")) flag = "DOWNLOAD_OK";
					
					else//arruma a nova porta e ip pra buscar em outro peer
					{
						msg.setPortaBusca(Integer.parseInt(buscarPeer.get(0)));
					}
				}
				
				
				//cria o file
				File arquivoLocal = new File(local + "\\" + msg.getElementoBusca());
				
				//cria o arquivo no path
				FileInputStream fileInput = new FileInputStream(arquivoLocal);
				
				//cria o leitor
				BufferedInputStream buffInput = new BufferedInputStream(fileInput);
				
				OutputStream os = no.getOutputStream();
				
				//divide em blocos de 4k
				byte[] data = new byte[4096];
				int count;
				count = buffInput.read(data);
				while (count != -1) 
				{
					os.write(data,0,count);
					count = buffInput.read(data);
				}
				
				os.flush();
				os.close();
				fileInput.close();
				buffInput.close();
				
				
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		}
	}

	public static class ThreadLoopDown extends Thread
	{
		ServerSocket serverTCPSocket;
		DatagramSocket clienteSocket;
		String local;
		
		public ThreadLoopDown(ServerSocket TCPSoquete, DatagramSocket serverSoquete, String localidade)
		{
			serverTCPSocket = TCPSoquete;
			clienteSocket = serverSoquete;
			local = localidade;
		}
		
		public void run()
		{
			while(serverTCPSocket.isClosed() != true)
			{
				try 
				{
					//cria o socket e fica aguardando
					Socket no = serverTCPSocket.accept();
					
					ThreadDownload TdDown = new ThreadDownload(no, clienteSocket,local);
					TdDown.start();
					
				} 
				catch (Exception e) 
				{
					//e.printStackTrace();
				}
			}
		}
	}
	
	public static class ThreadReqDownload extends Thread
	{
		Mensagem msg;
		String downIP;
		int downPorta;
		String downArquivo;
		
		public ThreadReqDownload(Mensagem message, String downip, int downPort, String downArquiv)
		{
			msg = message;
			downIP = downip;
			downPorta = downPort;
			downArquivo = downArquiv;
		}
		
		public void run()
		{
			funcaoDownload(msg,downIP,downPorta, downArquivo);
		}
	}
	
	public static class ThreadClienteLoopAlive extends Thread
	{
		DatagramSocket aliveSocket;
		
		public ThreadClienteLoopAlive(DatagramSocket alivSocket)
		{
			aliveSocket = alivSocket;
		}
		
		public void run()
		{
			try 
			{
			
				while(true)
				{		
					Gson gson = new Gson();
					
					//recebendo
					byte[] recebimento = new byte[1024];
					DatagramPacket recebeAlive = new DatagramPacket(recebimento, recebimento.length);
					aliveSocket.receive(recebeAlive);

					String mensagemUDP = new String(recebeAlive.getData(),recebeAlive.getOffset(),recebeAlive.getLength());//transforma a requisicao em string
					Mensagem msg = gson.fromJson(mensagemUDP,Mensagem.class);
					
					if(msg.getTipo().equals("ALIVE?"))
					{
						//enviando
						msg.setTipo("ALIVE_OK");
						byte[] msgEnvio = gson.toJson(msg).getBytes();
						DatagramPacket envioAlive = new DatagramPacket(msgEnvio, msgEnvio.length, recebeAlive.getAddress(), recebeAlive.getPort());
						aliveSocket.send(envioAlive);
					}
				}
				
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		}
	}
}

