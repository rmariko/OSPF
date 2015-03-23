# OSPF
An implementation of the shortest path algorithm 'Open Shortest Path First' for Internet Protocol networks.

## Build
Run `make` in the top level directory to compile java classes. 

## Usage
Usage: `java router <router_id> <nse_host> <nse_port> <router_port>`

where, 
* `router_id` is an integer that represents the router id. It should be unique for each router.
* `nse_host` is the host where the Network State Emulator is running.
* `nse_port` is the port number of the Network State Emulator.
* `router_port` is the router port
