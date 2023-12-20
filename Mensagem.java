

import java.net.InetAddress;
import java.util.ArrayList;

public class Mensagem {
	
	private String tipo;
	private InetAddress ip;
	private int porta;
	private int portaTCP;
	private String local;
	private String elementoBusca;
	private ArrayList<String> arquivos;
	private int portaServer;
	private int portaBusca;
	private String localBusca;
	private InetAddress ipBusca;
	private int portaAlive;
	
	public Mensagem(InetAddress ip, int porta, int portaTCP, int portaAlive)
	{
		this.ip = ip;
		this.porta = porta;
		this.portaTCP = portaTCP;
		this.portaServer = 10098;
		this.portaAlive = portaAlive;
	}
	
	public Mensagem(int portaTCP)
	{
		this.portaTCP = portaTCP;
	}
	
	public Mensagem(String tipo)
	{
		this.tipo = tipo;
	}
	
	public Mensagem(InetAddress ip, int porta, int portaTCP, String tipo)
	{
		this.ip = ip;
		this.porta = porta;
		this.tipo = tipo;
		this.portaTCP = portaTCP;
	}
	
	public Mensagem(InetAddress ip, int porta,int portaTCP, String local, String tipo)
	{
		this.ip = ip;
		this.porta = porta;
		this.local = local;
		this.tipo = tipo;
		this.portaTCP = portaTCP;
	}
	
	public Mensagem(InetAddress ip, int porta, String local, String tipo, ArrayList<String> arquivos)
	{
		this.ip = ip;
		this.porta = porta;
		this.local = local;
		this.tipo = tipo;
		this.arquivos = arquivos;
	}
	
	public int getPortaAlive() {
		return portaAlive;
	}
	
	public void setPortaAlive(int portaAlive) {
		this.portaAlive = portaAlive;
	}
	
	public int getPortaServer() {
		return portaServer;
	}
	public String getElementoBusca() {
		return elementoBusca;
	}
	public int getPortaTCP() {
		return portaTCP;
	}
	public ArrayList<String> getArquivos() {
		return arquivos;
	}
	public String getLocal() {
		return local;
	}
	public InetAddress getIp() {
		return ip;
	}
	public int getPorta() {
		return porta;
	}
	public String getTipo() {
		return tipo;
	}
	
	public void setPortaServer(int portaServer) {
		this.portaServer = portaServer;
	}
	public void setElementoBusca(String elementoBusca) {
		this.elementoBusca = elementoBusca;
	}
	public void setPortaTCP(int portaTCP) {
		this.portaTCP = portaTCP;
	}
	public void setArquivos(ArrayList<String> arquivos) {
		this.arquivos = arquivos;
	}
	public void setLocal(String local) {
		this.local = local;
	}
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}
	public void setPorta(int porta) {
		this.porta = porta;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public int getPortaBusca() {
		return portaBusca;
	}

	public void setPortaBusca(int portaBusca) {
		this.portaBusca = portaBusca;
	}

	public InetAddress getIpBusca() {
		return ipBusca;
	}

	public void setIpBusca(InetAddress ipBusca) {
		this.ipBusca = ipBusca;
	}

	public String getLocalBusca() {
		return localBusca;
	}

	public void setLocalBusca(String localBusca) {
		this.localBusca = localBusca;
	}

}
