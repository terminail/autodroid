package com.autodroid.test;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Pure Java mDNS discovery test using JmDNS library.
 * This test verifies that the autodroid server properly publishes its service through mDNS.
 * This is a pure Java implementation without any Android-specific code.
 */
public class PureJavaMdnsDiscoveryTest {
    private static final String SERVICE_TYPE = "_autodroid._tcp.local.";
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 3;
    
    private final List<DiscoveredService> discoveredServices = new ArrayList<>();
    private CountDownLatch discoveryLatch;
    
    public static class DiscoveredService {
        public String name;
        public String type;
        public String hostAddress;
        public int port;
        
        public DiscoveredService(String name, String type, String hostAddress, int port) {
            this.name = name;
            this.type = type;
            this.hostAddress = hostAddress;
            this.port = port;
        }
        
        @Override
        public String toString() {
            return "DiscoveredService{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", hostAddress='" + hostAddress + '\'' +
                    ", port=" + port +
                    '}';
        }
    }
    
    /**
     * Integration test that discovers autodroid server through mDNS.
     * This test acts as a client and verifies that the server is properly publishing its service.
     */
    public boolean testMdnsServiceDiscovery() {
        System.out.println("Starting pure Java mDNS service discovery test");
        discoveredServices.clear();
        
        JmDNS jmdns = null;
        try {
            // Create JmDNS instance
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            
            // Create service listener
            final JmDNS finalJmdns = jmdns;
            ServiceListener listener = new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    System.out.println("Service added: " + event.getName() + " (" + event.getType() + ")");
                    
                    // Request service info
                    finalJmdns.requestServiceInfo(event.getType(), event.getName());
                }
                
                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getName());
                }
                
                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Service resolved: " + event.getName());
                    
                    ServiceInfo info = event.getInfo();
                    if (info != null) {
                        String serviceName = info.getName();
                        String[] addresses = info.getHostAddresses();
                        int port = info.getPort();
                        
                        if (addresses != null && addresses.length > 0) {
                            String hostAddress = addresses[0];
                            System.out.println("Service details - Name: " + serviceName + 
                                    ", Host: " + hostAddress + ", Port: " + port);
                            
                            // Add to discovered services
                            DiscoveredService service = new DiscoveredService(
                                    serviceName, info.getType(), hostAddress, port);
                            discoveredServices.add(service);
                            
                            // Signal that we've discovered at least one service
                            if (discoveryLatch != null) {
                                discoveryLatch.countDown();
                            }
                        }
                    }
                }
            };
            
            // Add service listener
            jmdns.addServiceListener(SERVICE_TYPE, listener);
            System.out.println("Started browsing for services of type: " + SERVICE_TYPE);
            
            // Wait for service discovery with timeout
            discoveryLatch = new CountDownLatch(1);
            boolean discovered = discoveryLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!discovered) {
                System.err.println("Timeout waiting for mDNS service discovery");
                return false;
            }
            
            System.out.println("Service discovery completed successfully");
            
            // Verify we discovered at least one service
            if (discoveredServices.isEmpty()) {
                System.err.println("No services discovered");
                return false;
            }
            
            // Verify service details
            DiscoveredService service = discoveredServices.get(0);
            if (!service.type.equals(SERVICE_TYPE)) {
                System.err.println("Unexpected service type: " + service.type);
                return false;
            }
            
            if (service.port != 8004) {
                System.err.println("Unexpected service port: " + service.port);
                return false;
            }
            
            if (!service.name.contains("Autodroid Server")) {
                System.err.println("Unexpected service name: " + service.name);
                return false;
            }
            
            if (service.hostAddress == null || service.hostAddress.isEmpty()) {
                System.err.println("Service has no valid host address");
                return false;
            }
            
            System.out.println("Successfully discovered service: " + service.name);
            System.out.println("  Address: " + service.hostAddress);
            System.out.println("  Port: " + service.port);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error during service discovery: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Clean up
            if (jmdns != null) {
                try {
                    jmdns.close();
                } catch (IOException e) {
                    System.err.println("Error closing JmDNS: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Test that tries multiple attempts to discover the service.
     * This is useful if the service takes some time to register.
     */
    public boolean testMdnsServiceDiscoveryMultipleAttempts() {
        System.out.println("Starting multiple attempts test");
        
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            System.out.println("Attempt " + attempt + "/" + MAX_ATTEMPTS + " to discover service...");
            
            try {
                if (testMdnsServiceDiscovery()) {
                    System.out.println("Service discovered on attempt " + attempt);
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
            }
            
            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(5000); // Wait 5 seconds before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        System.err.println("Failed to discover service after " + MAX_ATTEMPTS + " attempts");
        return false;
    }
    
    /**
     * Run the test directly
     */
    public static void main(String[] args) {
        System.out.println("Running pure Java mDNS service discovery test...");
        PureJavaMdnsDiscoveryTest test = new PureJavaMdnsDiscoveryTest();
        
        boolean success = test.testMdnsServiceDiscoveryMultipleAttempts();
        
        if (success) {
            System.out.println("Test completed successfully!");
        } else {
            System.err.println("Test failed!");
            System.exit(1);
        }
    }
}