package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"netguard-tools/networkscan"
)

func main() {
	iface := flag.String("i", "wlan0", "Network interface to use")
	target := flag.String("target", "", "Target network (e.g. 192.168.1.0/24)")
	cutIp := flag.String("cut", "", "IP to cut off")
	gateway := flag.String("gateway", "", "Gateway IP")
	flag.Parse()

	if *target == "" && *cutIp == "" {
		fmt.Println("Usage: go run main.go -i <interface> -target <cidr> OR -cut <ip> -gateway <ip>")
		os.Exit(1)
	}

	// Graceful shutdown handling
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	var scanner *networkscan.NetworkScanner
	var err error

	// Initial setup with retry logic for intermittent connection
	for i := 0; i < 5; i++ {
		scanner, err = networkscan.NewNetworkScanner(*iface)
		if err == nil {
			break
		}
		fmt.Printf("Startup failed: %v. Retrying in 3 seconds (Attempt %d/5)...\n", err, i+1)
		time.Sleep(3 * time.Second)
	}

	if err != nil {
		fmt.Printf("Critical: Failed to initialize scanner after retries: %v\n", err)
		os.Exit(1)
	}
	defer scanner.Close()

	go func() {
		<-sigs
		fmt.Println("\nShutdown signal received. Closing...")
		scanner.Close()
		os.Exit(0)
	}()

	if *target != "" {
		devices, err := scanner.NetScan(*target)
		if err != nil {
			fmt.Printf("Scan error: %v. Check network_errors.log for details.\n", err)
		} else {
			fmt.Printf("Found %d devices.\n", len(devices))
		}
	}

	if *cutIp != "" && *gateway != "" {
		device := networkscan.Device{
			IP:  net.ParseIP(*cutIp),
			MAC: nil, // In a real scenario, we'd find the MAC first
		}
		
		// If MAC is missing, try to find it first via a targeted scan
		// (Simplified for this enhancement)
		
		fmt.Printf("Targeting IP: %s\n", *cutIp)
		scanner.CutOffDevice(device, *gateway)
	}
}
