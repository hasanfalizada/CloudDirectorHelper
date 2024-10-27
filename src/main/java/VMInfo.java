import java.util.HashMap;
import java.util.Map;

public class VMInfo {
    private String name;
    private String guestOs;
    private int numberOfCpus;
    private int memoryMB;
    private String status;
    private String networkName;
    private String ipAddress;
    private String dateCreated;
    private long totalStorageAllocatedMb;
    private String href;
    private Map<String, String> metadata;

    // Constructor
    public VMInfo(String name, String guestOs, int numberOfCpus, int memoryMB, String status,
                  String networkName, String ipAddress, String dateCreated,
                  long totalStorageAllocatedMb, String href) {
        this.name = name;
        this.guestOs = guestOs;
        this.numberOfCpus = numberOfCpus;
        this.memoryMB = memoryMB;
        this.status = status;
        this.networkName = networkName;
        this.ipAddress = ipAddress;
        this.dateCreated = dateCreated;
        this.totalStorageAllocatedMb = totalStorageAllocatedMb;
        this.href = href;
        this.metadata = new HashMap<>(); // Initialize metadata
    }

    // Getter for metadata
    public Map<String, String> getMetadata() {
        return metadata;
    }

    // Setter for metadata
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }


    // Getters
    public String getName() {
        return name;
    }

    public String getGuestOs() {
        return guestOs;
    }

    public int getNumberOfCpus() {
        return numberOfCpus;
    }

    public int getMemoryMB() {
        return memoryMB;
    }

    public String getStatus() {
        return status;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public long getTotalStorageAllocatedMb() {
        return totalStorageAllocatedMb;
    }

    public String getHref() {
        return href;
    }
}
