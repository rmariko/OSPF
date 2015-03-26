import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class router {
	
	//////////////////////////////////////// ROUTER CLASSES //////////////////////////////////////////////////
	private static int NBR_ROUTER = 5; 						/* for simplicity we consider only 5 routers */
	private static DatagramSocket router_socket;			/* Main socket used for communicating with the nse */
	
	/*  List of Lists containing the link state database */
	/*	-- Each index counts as router-id-1
	 * 	-- For each index, there is a list of link_costs containing router links and costs
	 */	
	private static List<List<link_cost>> Top_DB = new ArrayList<List<link_cost>>();

	/*  List of Lists containing the Routing Information Base */
	/*	-- Each index counts as router-id-1
	 * 	-- For each index, there is an integer signifying least cost for the path to a router (index-1)
	 */	
	private static Map<String, List<Integer>> RIB = new HashMap<String, List<Integer>>();
	
	// -- HELLO PDU PACKET --
	static class pkt_HELLO
	{
		public pkt_HELLO(int router_id, int link_id) {
			super();
			this.router_id = router_id;
			this.link_id = link_id;
		}
		int router_id; 		/* id of the router who sends the HELLO PDU */
		int link_id; 		/* id of the link through which it is sent */
		
		public int getRouter_id() {
			return router_id;
		}
		public void setRouter_id(int router_id) {
			this.router_id = router_id;
		}
		public int getLink_id() {
			return link_id;
		}
		public void setLink_id(int link_id) {
			this.link_id = link_id;
		}
		
		public static pkt_HELLO parseHelloData(DatagramPacket data) throws Exception {
			ByteBuffer buffer = ByteBuffer.wrap(data.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int router_id = buffer.getInt();
			int link_id = buffer.getInt();
			
			return new pkt_HELLO(router_id, link_id);
		}
	}
	
	// -- LS PDU PACKET --
	static class pkt_LSPDU
	{
		public pkt_LSPDU(int sender, int router_id, int link_id, int cost,
				int via) {
			super();
			this.sender = sender;
			this.router_id = router_id;
			this.link_id = link_id;
			this.cost = cost;
			this.via = via;
		}
		int sender; 		/* sender of the LS PDU */
		int router_id; 		/* router id */
		int link_id; 		/* link id */
		int cost;		 	/* cost of the link */
		int via; 			/* id of the link through which the LS PDU is sent */
		
		public int getSender() {
			return sender;
		}
		public void setSender(int sender) {
			this.sender = sender;
		}
		public int getRouter_id() {
			return router_id;
		}
		public void setRouter_id(int router_id) {
			this.router_id = router_id;
		}
		public int getLink_id() {
			return link_id;
		}
		public void setLink_id(int link_id) {
			this.link_id = link_id;
		}
		public int getCost() {
			return cost;
		}
		public void setCost(int cost) {
			this.cost = cost;
		}
		public int getVia() {
			return via;
		}
		public void setVia(int via) {
			this.via = via;
		}
		
		public static pkt_LSPDU parseLsData(DatagramPacket data) throws Exception {
			ByteBuffer buffer = ByteBuffer.wrap(data.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int sender = buffer.getInt();
			int router_id = buffer.getInt();
			int link_id = buffer.getInt();
			int cost = buffer.getInt();
			int via = buffer.getInt();
			
			return new pkt_LSPDU(sender, router_id, link_id, cost, via);
		}
	}
	
	// -- INIT PACKET --
	static class pkt_INIT
	{
		public pkt_INIT(int router_id) {
			super();
			this.router_id = router_id;
		}

		int router_id; 		/* id of the router that send the INIT PDU */

		public int getRouter_id() {
			return router_id;
		}

		public void setRouter_id(int router_id) {
			this.router_id = router_id;
		}
	}
	
	// -- LINK COST ASSOCIATED WITH A ROUTER --
	static class link_cost
	{
		public link_cost(int link, int cost) {
			super();
			this.link = link;
			this.cost = cost;
		}
		public link_cost() {
			// TODO Auto-generated constructor stub
		}
		int link; 			/* link id */
		int cost; 			/* associated cost */
		
		public int getLink() {
			return link;
		}
		public void setLink(int link) {
			this.link = link;
		}
		public int getCost() {
			return cost;
		}
		public void setCost(int cost) {
			this.cost = cost;
		}
	}
	
	// -- CIRCUIT DATABASE --
	static class circuit_DB
	{
		public circuit_DB(int nbr_link, link_cost[] linkcost) {
			super();
			this.nbr_link = nbr_link;
			this.linkcost = linkcost;
		}
		public circuit_DB() {
			// TODO Auto-generated constructor stub
		}
		int nbr_link; 		/* number of links attached to a router */
		link_cost[] linkcost = new link_cost[NBR_ROUTER]; 
							/* we assume that at most NBR_ROUTER links are attached to each router */
		public int getNbr_link() {
			return nbr_link;
		}
		public void setNbr_link(int nbr_link) {
			this.nbr_link = nbr_link;
		}
		public link_cost[] getLinkcost() {
			return linkcost;
		}
		public void setLinkcost(link_cost[] linkcost) {
			this.linkcost = linkcost;
		}
		
		public static circuit_DB parseCircuitData(DatagramPacket data) throws Exception {
			ByteBuffer buffer = ByteBuffer.wrap(data.getData());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int nbr_link = buffer.getInt();
			link_cost[] lc_list = new link_cost[nbr_link];
			for(int i=0; i < nbr_link; i++){
				link_cost lc = new link_cost(buffer.getInt(), buffer.getInt());
				lc_list[i] = lc;
			}
			
			return new circuit_DB(nbr_link, lc_list);
		}
		
		public void print_CDB() {
			System.out.printf("Circuit DB:\n");
			System.out.printf(" nbr_link: %d\n", nbr_link);
			for(int i=0; i < linkcost.length; i++) {
				System.out.printf(" link: %d cost: %d\n", linkcost[i].link, linkcost[i].cost);
			}
			System.out.print("\n");
		}
	}
	
	//////////////////////////////////////// HELPER FUNCTIONS ////////////////////////////////////////

	///// Write to a file /////
	public static void write_log(String fileName, String data) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			out.println(data);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	///// Construct RIB /////
	public static void print_rib(int router_id){
		System.out.println("# RIB");
		for(int i=0; i < NBR_ROUTER; i++){
			System.out.printf("R%d -> R%d -> ", router_id, i+1);
			List<Integer> x = RIB.get("R"+(i+1));
			for(int j=0; j < 2; j ++){
				if(j == 0){
					if(x.get(j) == Integer.MAX_VALUE) {
						System.out.print("INF, ");
					} else if(x.get(j) == 0) {
						System.out.print("local, ");
					} else {
						System.out.printf("R%d, ", x.get(j)+1);
					}
				} else {
					if(x.get(j) == Integer.MAX_VALUE) {
						System.out.println("INF");
					} else {
						System.out.printf("%d\n", x.get(j));
					}
				}
			}
		}
	}
	
	///// Construct Topology DB /////
	public static void print_top_dB(int router_id){
		System.out.println("# Topology Database");
		for(int i=0; i < NBR_ROUTER; i++) {
			List<link_cost> lc = Top_DB.get(i);
			if(lc != null){
				int nbr_link = lc.size();
				System.out.printf("R%d -> R%d nbr link %d\n", router_id, i+1, nbr_link);
				for(int j=0; j < nbr_link; j++) {
					System.out.printf("R%d -> R%d link %d cost %d\n", router_id, i+1, lc.get(j).getLink(), lc.get(j).getCost());
				}
			}
		}
	}
	
	///// Send initial packet /////
	public static void send_pkt(String nse_host, int nse_port, int[] r_data){
		try {
			InetAddress IPAddress = InetAddress.getByName(nse_host);
			
			System.out.printf("Sending for router [%d]\n", r_data[0]);
			
			// Create packet to be sent;
			ByteBuffer b = ByteBuffer.allocate(r_data.length*4);
			b.order(ByteOrder.LITTLE_ENDIAN);
			for(int i=0; i < r_data.length; i++) {
				b.putInt(r_data[i]);
			}
			
			// Send packet
			DatagramPacket sendPacket = new DatagramPacket(b.array(), b.array().length, IPAddress, nse_port);
			try {
				router_socket.send(sendPacket);
			} catch (IOException e) {
				// Do nothing
			}
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	///// Receive a packet /////
	public static DatagramPacket rcv_pkt() {
		ByteBuffer b = ByteBuffer.allocate(1024);
		
		DatagramPacket rcvPacket = new DatagramPacket(b.array(), b.array().length);
		
		// Read Datagram from the nse
		try {
			router_socket.receive(rcvPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		return rcvPacket;
	}
	
	///// Check to see if two LSPDU packets are the same
	public static Boolean same_ls_pkts(pkt_LSPDU pkt1, pkt_LSPDU pkt2) {
		if((pkt1.getRouter_id() != pkt2.getRouter_id()) || (pkt1.getLink_id() != pkt2.getLink_id()) || (pkt1.getCost() != pkt2.getCost())) {
			return false;
		}
		return true;	
	}
	
	///// Check to see if a given LSPDU packet has already been received /////
	public static Boolean duplicate_lspdu(pkt_LSPDU pkt, List<pkt_LSPDU> pkt_list) {
		if(pkt_list.isEmpty()) {
			return false;
		} else {
			for(int i=0; i < pkt_list.size(); i++) {
				if(same_ls_pkts(pkt, pkt_list.get(i))) {
					return true;
				}
			}
			return false;
		}
	}
	
	// Check to see if two link_costs are the same
	public static Boolean duplicate_link_cost(link_cost lc1, link_cost lc2){
		if(lc1.getCost() != lc2.getCost() || lc1.getLink() != lc2.getLink()) {
			return false;
		}
		return true;
	}
	
	public static Boolean lc_in_list(link_cost lc, List<link_cost> lc_list) {
		if(lc_list.isEmpty()) {
			return false;
		} else {
			for(int i=0; i < lc_list.size(); i++) {
				if(duplicate_link_cost(lc, lc_list.get(i))) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	
	////////////////////////////////////////////////// MAIN //////////////////////////////////////////////////
	public static void main(String args[]) {
		
		// Check for correct command line arguments
		if (args.length != 4){
			System.out.println("Usage: java router <router_id> <nse_host> <nse_port> <router_port>");
			return;
		}
		
		// Grab command line arguments
		int router_id = Integer.parseInt(args[0]);
		String nse_host = args[1];
		int nse_port = Integer.parseInt(args[2]);
		int router_port = Integer.parseInt(args[3]);
		
		// Create Datagram socket
		try {
			router_socket = new DatagramSocket(router_port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		// Initialize RIB
		for(int i=0; i < NBR_ROUTER; i++){
			if(i+1 != router_id) {
				ArrayList<Integer> rib = new ArrayList<Integer>() {{ 
					add(Integer.MAX_VALUE);
					add(Integer.MAX_VALUE);
				}};
				RIB.put("R"+(i+1), rib);
			} else {
				ArrayList<Integer> rib = new ArrayList<Integer>() {{ 
					add(0);
					add(0);
				}};
				RIB.put("R"+(i+1), rib);
			}
		}
		
		// Initialize LSB
		for(int i=0; i < NBR_ROUTER; i++){
			Top_DB.add(null);
		}
		
		// #1 -- Send INIT packet
		int[] init_data = new int[1];
		init_data[0] = router_id;
		send_pkt(nse_host, nse_port, init_data);
		
		// #2 -- Wait for circuit DB
		circuit_DB c_db = new circuit_DB();
		try {
			c_db = circuit_DB.parseCircuitData(rcv_pkt());
			// Add to Topology DB
			List<link_cost> lc_list = new ArrayList<link_cost>();
			for(int i=0; i < c_db.getNbr_link(); i++) {
				link_cost local_lc = new link_cost(c_db.getLinkcost()[i].getLink(), 
						c_db.getLinkcost()[i].getCost());
				lc_list.add(local_lc);
			}
			Top_DB.set(router_id-1, lc_list);
			
			// Print the Topology DB
			print_top_dB(router_id);
			
			// Print the CDB
			c_db.print_CDB();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		// PRINT THE RIB
		print_rib(router_id);
				
		// #3 -- Send Hello PDU to all neighbours
		int[] hello_pdu = new int[2];
		hello_pdu[0] = router_id;
		for(int i=0; i < c_db.getNbr_link(); i++) {
			hello_pdu[1] = c_db.linkcost[i].link;
			send_pkt(nse_host, nse_port, hello_pdu);
		}
		
		// #4 -- Wait to receive a Hello PDU or LS PDU from neighbours
		// Create list for hello packets received
		Map<String, Integer> rcvd_hello = new HashMap<String, Integer>();
		List<pkt_LSPDU> rcvd_ls = new ArrayList<pkt_LSPDU>();
		while (true) {
			// Wait for pkt
			DatagramPacket rcv_pdu = rcv_pkt();
			try {	
				///// --- RECEIVED HELLO PACKET --- /////
				if(rcv_pdu.getLength() == 8){
					pkt_HELLO hello_pkt = pkt_HELLO.parseHelloData(rcv_pdu);
					// Add to received list
					String sender = "R"+hello_pkt.getRouter_id();
					Integer sender_link = hello_pkt.getLink_id();
					rcvd_hello.put(sender, sender_link);
					
					System.out.printf("Received pkt_HELLO from R%d via link %d\n", hello_pkt.getRouter_id(), hello_pkt.getLink_id());
					// This is a HELLO PDU packet; send set of LS_PDU packets to this neighbor
					int[] ls_pdu = new int[5];
					ls_pdu[0] = router_id;
					ls_pdu[1] = router_id;
					for(int i=0; i < c_db.getNbr_link(); i++) {
						ls_pdu[2] = c_db.getLinkcost()[i].getLink();
						ls_pdu[3] = c_db.getLinkcost()[i].getCost();
						ls_pdu[4] = hello_pkt.getLink_id();
						send_pkt(nse_host, nse_port, ls_pdu);
					}
				///// --- RECEIVED LSPDU PACKET --- /////
				} else {
					// 1 -- Update Topology Database
					pkt_LSPDU lspdu_pkt = pkt_LSPDU.parseLsData(rcv_pdu);
					
					System.out.printf("Recieved lspdu packet from %d via link %d\n", lspdu_pkt.getSender(), lspdu_pkt.getVia());
					System.out.printf(" R%d -- link: %d cost: %d\n", lspdu_pkt.getRouter_id(), lspdu_pkt.getLink_id(), lspdu_pkt.getCost());
					
					// First check to see that this is NOT a duplicate lspdu
					if(!duplicate_lspdu(lspdu_pkt, rcvd_ls)){
						rcvd_ls.add(lspdu_pkt);
						
						List<link_cost> temp_lc_list;
						if(Top_DB.get(lspdu_pkt.getSender()-1) != null){
							temp_lc_list = Top_DB.get(lspdu_pkt.getSender()-1);
						} else {
							temp_lc_list = new ArrayList<link_cost>();
						}
						
						link_cost lc = new link_cost(lspdu_pkt.getLink_id(), lspdu_pkt.getCost());
						// Add to topology DB if not already there
						if(!lc_in_list(lc, temp_lc_list)){
							temp_lc_list.add(lc);
							Top_DB.set(lspdu_pkt.getSender()-1, temp_lc_list);
						}
						
						// 2 -- Send to all neighbours this unique LSPDU (minus the one who sent the LSPDU)
						for(int i=0; i < c_db.getNbr_link(); i++) {
							int link_to_send = c_db.getLinkcost()[i].getLink();
							int link_from_lspdu = lspdu_pkt.getVia();
							if(link_to_send != link_from_lspdu) {
								// I have not received a HELLO pkt from this neighbour yet and they are
								// not the one who sent the LSPDU pkt.
								int send_lspdu[] = new int[] {
										router_id, 
										lspdu_pkt.getRouter_id(), 
										lspdu_pkt.getLink_id(),
										lspdu_pkt.getCost(),
										link_to_send};
								// Forward the packet
								System.out.println("Forwarding LSPDU PKT!");
								send_pkt(nse_host, nse_port, send_lspdu);
							}
						}
					}
				}
				// PRINT THE TOPOLOGY DB
				print_top_dB(router_id);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}