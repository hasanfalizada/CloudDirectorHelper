import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {

        HTMLGenerator htmlGenerator = new HTMLGenerator();
        List<VMInfo> vmList = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.print("Enter Cloud Director URL: ");
            String baseURL = scanner.nextLine();
            System.out.print("Enter API username: ");
            String username = scanner.nextLine();
            System.out.print("Enter API password: ");
            String password = scanner.nextLine();
            System.out.print("Enter the directory path where you want to save files: ");
            String directoryPath = scanner.nextLine();

            CloudClient client = new CloudClient(baseURL);

            // Step 1: Login
            if (client.login(username, password)) {
                // Step 2: Retrieve VM list
                int page = 1;
                boolean morePages = true;
                while (morePages) {
                    String vmResponse = client.getVMs(page);
                    if (vmResponse != null) {
                        List<VMInfo> vms = parseVMs(vmResponse);  // Implement a method to parse VMs from JSON
                        vmList.addAll(vms);
                        morePages = hasMorePages(vmResponse);  // Implement method to check for next page
                        page++;
                    } else {
                        morePages = false;
                    }
                }

                // Step 3: Fetch metadata for each VM
                for (VMInfo vm : vmList) {
                    String metadataResponse = client.getMetadata(vm.getHref());
                    if (metadataResponse != null) {
                        vm.setMetadata(parseMetadata(metadataResponse));  // Implement method to parse metadata
                    }
                }

                // Step 4: Generate HTML
                htmlGenerator.generateHTML(vmList, directoryPath);
            }
        } catch (Exception e) {
            System.err.println("An error occurred!");
            e.printStackTrace();
        }
    }

    public static List<VMInfo> parseVMs(String jsonResponse) {
        List<VMInfo> vmList = new ArrayList<>();

        // Parse the JSON response
        JsonElement jsonElement = JsonParser.parseString(jsonResponse);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Check if the response contains the "record" array
        JsonArray records = jsonObject.getAsJsonArray("record");
        for (JsonElement recordElement : records) {
            JsonObject record = recordElement.getAsJsonObject();

            String href = record.get("href").getAsString();
            String name = record.has("name") && !record.get("name").isJsonNull() ? record.get("name").getAsString() : "unknown";
            String guestOs = record.has("guestOs") && !record.get("guestOs").isJsonNull() ? record.get("guestOs").getAsString() : "unknown";
            int numberOfCpus = record.has("numberOfCpus") && !record.get("numberOfCpus").isJsonNull() ? record.get("numberOfCpus").getAsInt() : 0;
            int memoryMB = record.has("memoryMB") && !record.get("memoryMB").isJsonNull() ? record.get("memoryMB").getAsInt() : 0;
            String status = record.has("status") && !record.get("status").isJsonNull() ? record.get("status").getAsString() : "unknown";
            String networkName = record.has("networkName") && !record.get("networkName").isJsonNull() ? record.get("networkName").getAsString() : "unknown";
            String ipAddress = record.has("ipAddress") && !record.get("ipAddress").isJsonNull() ? record.get("ipAddress").getAsString() : "unknown";
            String dateCreated = record.has("dateCreated") && !record.get("dateCreated").isJsonNull() ? record.get("dateCreated").getAsString() : "unknown";
            long totalStorageAllocatedMb = record.has("totalStorageAllocatedMb") && !record.get("totalStorageAllocatedMb").isJsonNull() ? record.get("totalStorageAllocatedMb").getAsLong() : 0;


            // Create VMInfo object and add it to the list
            VMInfo vmInfo = new VMInfo(name, guestOs, numberOfCpus, memoryMB, status, networkName, ipAddress, dateCreated, totalStorageAllocatedMb, href);
            vmList.add(vmInfo);
        }

        return vmList;
    }

    public static boolean hasMorePages(String jsonResponse) {
        // Parse the JSON response
        JsonElement jsonElement = JsonParser.parseString(jsonResponse);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Check for the "link" array in the JSON
        JsonArray links = jsonObject.getAsJsonArray("link");

        // Iterate through the links to find a "nextPage" relation
        for (JsonElement linkElement : links) {
            JsonObject link = linkElement.getAsJsonObject();
            if (link.has("rel") && link.get("rel").getAsString().equals("nextPage")) {
                return true; // There is a next page
            }
        }

        return false; // No next page found
    }

    public static Map<String, String> parseMetadata(String metadataResponse) {
        Map<String, String> metadataMap = new HashMap<>();

        JsonElement jsonElement = JsonParser.parseString(metadataResponse);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonArray metadataEntries = jsonObject.getAsJsonArray("metadataEntry");
        for (JsonElement entryElement : metadataEntries) {
            JsonObject entry = entryElement.getAsJsonObject();
            String key = entry.get("key").getAsString();
            String value = entry.get("typedValue").getAsJsonObject().get("value").getAsString();
            metadataMap.put(key, value);
        }

        return metadataMap;
    }


}
