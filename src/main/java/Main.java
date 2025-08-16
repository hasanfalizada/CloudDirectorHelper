import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static String debugDirectory = "";
    private static PrintWriter debugWriter = null;

    public static void setDebugDirectory(String directory) {
        debugDirectory = directory;
        try {
            if (debugWriter != null) {
                debugWriter.close();
            }
            debugWriter = new PrintWriter(new FileWriter(debugDirectory + "\\debug_log.txt", true));
            writeDebug("=== Debug Log Started at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ===");
        } catch (IOException e) {
            System.err.println("Failed to create debug file: " + e.getMessage());
        }
    }

    private static void writeDebug(String message) {
        if (debugWriter != null) {
            debugWriter.println(message);
            debugWriter.flush(); // Ensure immediate write
        }
    }

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

            // Initialize debug logging
            setDebugDirectory(directoryPath);
            writeDebug("Starting VM data collection...");
            writeDebug("Cloud Director URL: " + baseURL);
            writeDebug("Username: " + username);

            CloudClient client = new CloudClient(baseURL);

            // Step 1: Login
            if (client.login(username, password)) {
                writeDebug("Login successful");

                // Step 2: Retrieve VM list
                int page = 1;
                boolean morePages = true;
                while (morePages) {
                    writeDebug("Fetching VMs page: " + page);
                    String vmResponse = client.getVMs(page);
                    if (vmResponse != null) {
                        List<VMInfo> vms = parseVMs(vmResponse);
                        vmList.addAll(vms);
                        writeDebug("Found " + vms.size() + " VMs on page " + page);
                        morePages = hasMorePages(vmResponse);
                        page++;
                    } else {
                        writeDebug("No response for page " + page);
                        morePages = false;
                    }
                }

                writeDebug("Total VMs found: " + vmList.size());

                // Step 3: Fetch metadata for each VM
                int vmCount = 0;
                for (VMInfo vm : vmList) {
                    vmCount++;
                    writeDebug("\n--- Processing VM " + vmCount + "/" + vmList.size() + ": '" + vm.getName() + "' ---");
                    writeDebug("VM '" + vm.getName() + "' - HREF: " + vm.getHref());

                    String metadataResponse = client.getMetadata(vm.getHref());
                    if (metadataResponse != null) {
                        writeDebug("VM '" + vm.getName() + "' - Raw metadata response:");
                        writeDebug(metadataResponse);

                        Map<String, String> metadata = parseMetadata(metadataResponse, vm.getName());
                        vm.setMetadata(metadata);

                        writeDebug("VM '" + vm.getName() + "' - Final parsed metadata summary:");
                        for (Map.Entry<String, String> entry : metadata.entrySet()) {
                            writeDebug("  " + entry.getKey() + " = '" + entry.getValue() + "'");
                        }

                        // Check specifically for the fields we're interested in
                        String[] importantFields = {"Project", "Owner", "Environment", "Severity", "Purpose"};
                        writeDebug("VM '" + vm.getName() + "' - Important fields check:");
                        for (String field : importantFields) {
                            String value = metadata.getOrDefault(field, "NOT_FOUND");
                            writeDebug("  " + field + ": " + (value.isEmpty() ? "EMPTY" : "'" + value + "'"));
                        }
                    } else {
                        writeDebug("VM '" + vm.getName() + "' - No metadata response received");
                    }
                    writeDebug("--- End processing VM: '" + vm.getName() + "' ---\n");
                }

                writeDebug("\n=== Generating reports ===");
                // Step 4: Generate HTML
                htmlGenerator.generateHTML(vmList, directoryPath);

                writeDebug("Process completed successfully");
            } else {
                writeDebug("Login failed");
            }
        } catch (Exception e) {
            writeDebug("ERROR occurred: " + e.getMessage());
            writeDebug("Stack trace:");
            e.printStackTrace(debugWriter);
            System.err.println("An error occurred! Check debug_log.txt for details.");
        } finally {
            if (debugWriter != null) {
                writeDebug("=== Debug Log Ended ===\n");
                debugWriter.close();
            }
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

    public static Map<String, String> parseMetadata(String metadataResponse, String vmName) {
        Map<String, String> metadataMap = new HashMap<>();

        try {
            writeDebug("=== Parsing metadata for VM: " + vmName + " ===");
            JsonElement jsonElement = JsonParser.parseString(metadataResponse);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (jsonObject.has("metadataEntry") && !jsonObject.get("metadataEntry").isJsonNull()) {
                JsonArray metadataEntries = jsonObject.getAsJsonArray("metadataEntry");
                writeDebug("VM '" + vmName + "' - Found " + metadataEntries.size() + " metadata entries");

                for (JsonElement entryElement : metadataEntries) {
                    JsonObject entry = entryElement.getAsJsonObject();

                    if (entry.has("key") && !entry.get("key").isJsonNull()) {
                        String rawKey = entry.get("key").getAsString();
                        String key = rawKey.trim(); // TRIM WHITESPACE FROM KEY
                        String value = "";

                        // Handle different possible value structures
                        if (entry.has("typedValue") && !entry.get("typedValue").isJsonNull()) {
                            JsonObject typedValue = entry.get("typedValue").getAsJsonObject();
                            if (typedValue.has("value") && !typedValue.get("value").isJsonNull()) {
                                value = typedValue.get("value").getAsString();
                                if (value != null) {
                                    value = value.trim(); // ALSO TRIM WHITESPACE FROM VALUE
                                }
                            }
                        } else if (entry.has("value") && !entry.get("value").isJsonNull()) {
                            // Alternative structure where value is directly under entry
                            value = entry.get("value").getAsString();
                            if (value != null) {
                                value = value.trim(); // ALSO TRIM WHITESPACE FROM VALUE
                            }
                        }

                        metadataMap.put(key, value);
                        writeDebug("VM '" + vmName + "' - Metadata entry - Raw Key: '" + rawKey + "', Trimmed Key: '" + key + "', Value: '" + value + "'");
                    }
                }
            } else {
                writeDebug("VM '" + vmName + "' - No metadataEntry array found in response");
            }
        } catch (Exception e) {
            writeDebug("VM '" + vmName + "' - Error parsing metadata: " + e.getMessage());
            e.printStackTrace(debugWriter);
        }

        writeDebug("VM '" + vmName + "' - Total metadata entries parsed: " + metadataMap.size());
        return metadataMap;
    }
}