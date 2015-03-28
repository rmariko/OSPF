import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class router {
	
	//////////////////////////////////////// ROUTER CLASSES //////////////////////////////////////////////////
	private static int NBR_ROUTER = 5; 						/* for simplicity we consider only 5 routers */
	private static DatagramSocket router_socket;			/* Main socket used for communicating with the nse */
	private static int MAX_INT = 500;
	
	/*  List of Lists containing the link state database */
	/*	-- Each index counts as router-id-1
	 * 	-- For each index, there is a list of link_costs containing router links and costs
	 */	
	private static List<List<link_cost>> Top_DB = new ArrayList<List<link_cost>>();

	/*  Map containing the Routing Information Base */
	/*	-- Each Key corresponds to a router
	 * 	-- Each Value corresponds to a mapping of the router's predecessor and lowest current cost
	 */	
	private static Map<String, Map<String, Integer>> RIB = new HashMap<String, Map<String, Integer>>();
	
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
	
	///// Write RIB to Log file /////
	public static void write_rib_to_log(String fileName, int router_id) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			out.println("# RIB");
			for(int i=0; i < NBR_ROUTER; i++){
				out.printf("R%d -> R%d -> ", router_id, i+1);
				String r = ("R"+ (i+1));
				Map<String, Integer> x = RIB.get(r);
				if(x == null) {
					out.println("INF, INF");
				} else {
					Set<String> set = RIB.get(r).keySet();
					for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
				        String s = it.next();
				        out.printf("%s, %d\n", s, RIB.get(r).get(s));
				    }
				}
			}
			out.println();
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
			String r = ("R"+ (i+1));
			Map<String, Integer> x = RIB.get(r);
			if(x == null) {
				System.out.println("INF, INF");
			} else {
				Set<String> set = RIB.get(r).keySet();
				for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
			        String s = it.next();
			        System.out.printf("%s, %d\n", s, RIB.get(r).get(s));
			    }
			}
		}
	}
	
	///// Write Topology DB to log file /////
	public static void write_tdb_to_log(String fileName, int router_id) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			out.println("# Topology Database");
			for(int i=0; i < NBR_ROUTER; i++) {
				List<link_cost> lc = Top_DB.get(i);
				if(lc != null){
					int nbr_link = lc.size();
					out.printf("R%d -> R%d nbr link %d\n", router_id, i+1, nbr_link);
					for(int j=0; j < nbr_link; j++) {
						out.printf("R%d -> R%d link %d cost %d\n", router_id, i+1, lc.get(j).getLink(), lc.get(j).getCost());
					}
				}
			}
			out.println();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
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
			return rcvPacket;
		} catch (SocketTimeoutException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
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
	public static Integer duplicate_link_cost(link_cost lc1, link_cost lc2){
		if(lc1.getCost() == lc2.getCost() && lc1.getLink() == lc2.getLink()) {
			return lc2.getCost();
		}
		return -1;
	}
	
	// Check to see if a given link_cost exists in the given list
	public static Integer lc_in_list(link_cost lc, List<link_cost> lc_list) {
		if(lc_list.isEmpty()) {
			return -1;
		} else {
			for(int i=0; i < lc_list.size(); i++) {
				int duplicate = duplicate_link_cost(lc, lc_list.get(i));
				if(duplicate != -1) {
					return duplicate;
				}
			}
			return -1;
		}
	}
	
	////////// HELPERS FOR DIJKSTRA'S ALGORITHM //////////
	
	// Check for adjacency between to routers. If adjacent, give the cost of the adj link
	public static Integer adjacent_cost(List<link_cost> link_costs, List<link_cost> lc) {
		for(int i=0; i < link_costs.size(); i++){
			for(int j=0; j < lc.size(); j++) {
				if(link_costs.get(i).getLink() == lc.get(j).getLink()) {
					return link_costs.get(i).getCost();
				}
			}
		}
		return -1;
	}
	
	// Function to initialize the shortest_path costs for each router
	public static Map<String, Integer> initialize_sp(int router_id, circuit_DB c_db) {
		Map<String,Integer> costs = new HashMap<String, Integer>();
		for(int i=0; i < NBR_ROUTER; i++) {
			if(i == router_id-1) {
				costs.put("R"+router_id, 0);
			} else if(Top_DB.get(i) != null && adjacent_cost(new ArrayList<link_cost>(Arrays.asList(c_db.getLinkcost())), Top_DB.get(i)) != -1){
				costs.put("R"+(i+1), adjacent_cost(new ArrayList<link_cost>(Arrays.asList(c_db.getLinkcost())), Top_DB.get(i)));
			} else {
				costs.put("R"+(i+1), MAX_INT);
			}
		}
		return costs;
	}
	
	////////////////////////////////////////////////// MAIN //////////////////////////////////////////////////
	public static void main(String args[]) throws SocketTimeoutException {
		
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
		
		// Filename for log file
		String filename = "router"+router_id+".log";
		
		// Create Datagram socket
		try {
			router_socket = new DatagramSocket(router_port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		// Initialize the Topology Database
		for(int i=0; i < NBR_ROUTER; i++){
			Top_DB.add(null);
		}
		
		// #1 -- Send INIT packet
		int[] init_data = new int[1];
		init_data[0] = router_id;
		send_pkt(nse_host, nse_port, init_data);
		
		// Write to log file
		String d1 = String.format("R%d -> Sending init packet for R%d", router_id, router_id);
		write_log(filename, d1);
		
		// #2 -- Wait for circuit DB
		circuit_DB c_db = new circuit_DB();
		try {
			c_db = circuit_DB.parseCircuitData(rcv_pkt());
			
			// Write to log
			String d2 = String.format("R%d -> Received circuit database", router_id);
			write_log(filename, d2);
			
			// Add to Topology DB
			List<link_cost> lc_list = new ArrayList<link_cost>();
			for(int i=0; i < c_db.getNbr_link(); i++) {
				link_cost local_lc = new link_cost(c_db.getLinkcost()[i].getLink(), 
						c_db.getLinkcost()[i].getCost());
				lc_list.add(local_lc);
			}
			Top_DB.set(router_id-1, lc_list);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		// Write updated T_DB to log
		write_tdb_to_log(filename, router_id);
		
		
		// Initalize RIB with local router
		Map<String, Integer> local_rib = new HashMap<String, Integer>();
		local_rib.put("Local", 0);
		RIB.put("R"+router_id, local_rib);
		
		// Write updated RIB to log
		write_rib_to_log(filename, router_id);
		
		// Set the socket timeout now
		try {
			router_socket.setSoTimeout(8000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
				
		// #3 -- Send Hello PDU to all neighbours
		int[] hello_pdu = new int[2];
		hello_pdu[0] = router_id;
		for(int i=0; i < c_db.getNbr_link(); i++) {
			hello_pdu[1] = c_db.linkcost[i].link;
			send_pkt(nse_host, nse_port, hello_pdu);
			
			// Write to log
			String d3 = String.format("R%d -> Sending hello packet across link %d\n", router_id, c_db.linkcost[i].link);
			write_log(filename, d3);
		}
		
		// #4 -- Wait to receive a Hello PDU or LS PDU from neighbours
		// Create list for hello packets received
		Map<String, Integer> rcvd_hello = new HashMap<String, Integer>();
		List<pkt_LSPDU> rcvd_ls = new ArrayList<pkt_LSPDU>();
		
		// Costs to each router in the network
		Map<String, Integer> sp_costs = initialize_sp(router_id, c_db);
		
		// Predecessor of each router to this current router_id
		Map<String, String> predecessors = new HashMap<String, String>();
		predecessors.put("R"+router_id, "Local");
		
		// Map of adjacent routers
		Map<String, Map<String, Integer>> adj = new HashMap<String, Map<String, Integer>>();
		
		////////// MAIN LOOP TO RECEIVE PACKETS //////////
		while (true) {
			try {	
				// Wait for pkt
				DatagramPacket rcv_pdu = rcv_pkt();
				
				// We have reached a timeout
				if(rcv_pdu == null) {
					router_socket.close();
					return;
				}
				
				///// --- RECEIVED HELLO PACKET --- /////
				if(rcv_pdu.getLength() == 8){
					pkt_HELLO hello_pkt = pkt_HELLO.parseHelloData(rcv_pdu);
					
					// Add to received list
					String sender = "R"+hello_pkt.getRouter_id();
					Integer sender_link = hello_pkt.getLink_id();
					rcvd_hello.put(sender, sender_link);
					
					// Write to log
					String d4 = String.format("R%d -> Received pkt_HELLO from R%d via link %d\n", router_id, hello_pkt.getRouter_id(), hello_pkt.getLink_id());
					write_log(filename, d4);
					
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
					int sender = lspdu_pkt.getSender();
					
					String d5 = String.format("R%d -> Recieved lspdu packet from %d via link %d\n", router_id, lspdu_pkt.getSender(), lspdu_pkt.getVia());
					String d6 = String.format("R%d -> LSPDU R%d -- link: %d cost: %d\n", router_id, lspdu_pkt.getRouter_id(), lspdu_pkt.getLink_id(), lspdu_pkt.getCost());
					
					write_log(filename, d5);
					write_log(filename, d6);
					
					// First check to see that this is NOT a duplicate lspdu
					if(!duplicate_lspdu(lspdu_pkt, rcvd_ls)){
						rcvd_ls.add(lspdu_pkt);
				
						List<link_cost> temp_lc_list;
						if(Top_DB.get(lspdu_pkt.getRouter_id()-1) != null){
							temp_lc_list = Top_DB.get(lspdu_pkt.getRouter_id()-1);
						} else {
							temp_lc_list = new ArrayList<link_cost>();
						}
						
						link_cost lc = new link_cost(lspdu_pkt.getLink_id(), lspdu_pkt.getCost());
						// Add to topology DB if not already there
						if(lc_in_list(lc, temp_lc_list) < 0){
							temp_lc_list.add(lc);
							Top_DB.set(lspdu_pkt.getRouter_id()-1, temp_lc_list);

							// Write updated Top DB to log and RIB
							write_tdb_to_log(filename, router_id);
							write_rib_to_log(filename, router_id);
							
							// ==== Dijikstra's algorithm ====
							link_cost ls_pkt_lc = new link_cost(lspdu_pkt.getLink_id(), lspdu_pkt.getCost());
							List<link_cost> w = new ArrayList<link_cost>();
							w.add(ls_pkt_lc);
							for(int j=0; j < NBR_ROUTER; j++) {
								if(j != lspdu_pkt.getRouter_id()-1 && Top_DB.get(j) != null) {
									List<link_cost> n = Top_DB.get(j);
									int adj_cost = adjacent_cost(w, n);
									if(adj_cost > 0) {
										String pre_pkt = predecessors.get("R"+lspdu_pkt.getRouter_id());
										String pre_adj = predecessors.get("R"+(j+1));
										
										if(pre_pkt == null) {
											predecessors.put("R"+lspdu_pkt.getRouter_id(), "R"+sender);
										}
										
										/////// Add adj router for both routers ///////
										Map<String, Integer> adj_r1;
										Map<String, Integer> adj_r2;
										if(adj.get("R"+(lspdu_pkt.getRouter_id())) != null) {
											adj_r1 = adj.get("R"+(lspdu_pkt.getRouter_id()));
										} else {
											adj_r1 = new HashMap<String, Integer>();
										}
										
										if(adj.get("R"+(j+1)) != null) {
											adj_r2 = adj.get("R"+(j+1));
										} else {
											adj_r2 = new HashMap<String, Integer>();
										}
										
										// Add to each map the adj router and link cost
										String r1 = "R" + lspdu_pkt.getRouter_id();
										int r1_cost = adj_cost;
										String r2 = "R" + (j+1);
										int r2_cost = adj_cost;
										
										adj_r1.put(r2, r2_cost);
										adj_r2.put(r1,  r1_cost);
										
										adj.put(r1, adj_r1);
										adj.put(r2, adj_r2);
										
										// Set cost for this new router
										int ls_pkt_cost = sp_costs.get("R"+lspdu_pkt.getRouter_id());
										int lspdu_cost = Math.min(ls_pkt_cost, sp_costs.get("R"+(j+1)) + adj_cost);
										if(lspdu_cost != ls_pkt_cost && lspdu_cost < MAX_INT) {
											
											// We have updated the router cost, so update predecessor
											if(pre_adj != "Local"){
												predecessors.put("R"+lspdu_pkt.getRouter_id(), pre_adj);
											} else {
												// Pre is local so set to sender
												predecessors.put("R"+lspdu_pkt.getRouter_id(), "R"+sender);
											}
											
											// Update Cost
											sp_costs.put("R"+lspdu_pkt.getRouter_id(), lspdu_cost);
											
											// Update RIB
											Map<String, Integer> router_rib = new HashMap<String, Integer>();
											router_rib.put(predecessors.get("R"+lspdu_pkt.getRouter_id()), sp_costs.get("R"+lspdu_pkt.getRouter_id()));
											RIB.put("R"+lspdu_pkt.getRouter_id(), router_rib);
										}
										
										
										
										
										// Write updated RIB to log file
										write_rib_to_log(filename, router_id);
										
										// Run through all current nodes and update costs accordingly
										for(int x=0; x < NBR_ROUTER; x++) {
											if(sp_costs.get("R"+(x+1)) < MAX_INT) {
												// Update this router
												if(adj.get("R"+(x+1)) != null) {
													String current_predecessor = predecessors.get("R"+(x+1));
													for(int y=0; y < adj.get("R"+(x+1)).size(); y++) {
														String[] keys = adj.get("R"+(x+1)).keySet().toArray(new String[0]);
														String adj_router_id = keys[y];
														int adj_link_cost = adj.get("R"+(x+1)).get(adj_router_id);
														int current_cost = sp_costs.get("R"+(x+1));
														int adj_current_cost = sp_costs.get(adj_router_id);
														int min_cost = Math.min(adj_current_cost, (current_cost+adj_link_cost));
														
														// Make sure we aren't updating costs for routers not attached to network yet
														if(min_cost < MAX_INT) {
															sp_costs.put(adj_router_id, min_cost);
															if(min_cost != adj_current_cost) {
																if(current_predecessor != "Local") {
																	predecessors.put(adj_router_id, current_predecessor);
																	System.out.printf("putting pre: %s for %s", adj_router_id, current_predecessor);
																} else {
																	predecessors.put(adj_router_id, adj_router_id);
																	System.out.printf("putting pre: %s for %s", adj_router_id, adj_router_id);
																}
															}
															
															// Update RIB
															Map<String, Integer> rib = new HashMap<String, Integer>();
															rib.put(predecessors.get(adj_router_id), sp_costs.get(adj_router_id));
															RIB.put(adj_router_id, rib);
														}
														
														
													}
												}
											}
										}
									}
								}
							}
							// Write updated RIB to log file
							write_rib_to_log(filename, router_id);
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
								send_pkt(nse_host, nse_port, send_lspdu);
								
								// Write sent packet to log
								String d7 = String.format("R%d -> Forwarding lspdu_pkt", router_id);
								String d8 = String.format(" Sender: %d, Router: %d, Link: %d, Cost: %d, Via Link: %d\n", 
										send_lspdu[0], send_lspdu[1], send_lspdu[2], send_lspdu[3], send_lspdu[4]);
								
								write_log(filename, d7);
								write_log(filename, d8);
							}
						}
					}
				}
				
			} catch (SocketTimeoutException e) {
				router_socket.close();
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}