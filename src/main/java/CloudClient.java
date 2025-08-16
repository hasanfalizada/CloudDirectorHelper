import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class CloudClient {
    private String baseURL;
    private String tokenType;
    private String accessToken;
    private OkHttpClient client;
    private ObjectMapper objectMapper;

    public CloudClient(String baseUrl) {
        this.baseURL = baseUrl;
        client = new OkHttpClient();
        objectMapper = new ObjectMapper();
    }

    public boolean login(String username, String password) throws IOException {
        String url = baseURL + "/sessions";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/*+json;version=37.2")
                .addHeader("Authorization", Credentials.basic(username, password))
                .post(RequestBody.create("", null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Headers headers = response.headers();
                accessToken = headers.get("X-VMWARE-VCLOUD-ACCESS-TOKEN");
                tokenType = headers.get("X-VMWARE-VCLOUD-TOKEN-TYPE");
                System.out.println("Logged in successfully. Access Token retrieved.");
                return true;
            } else {
                System.err.println("Login failed with code: " + response.code());
                return false;
            }
        }
    }

    public String getAuthHeader() {
        return tokenType + " " + accessToken;
    }

    public String getVMs(int page) throws IOException {
        String url = baseURL + "/vms/query?page=" + page + "&pageSize=25&format=records";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/*+json;version=37.2")
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                System.err.println("Failed to retrieve VMs with code: " + response.code());
                return null;
            }
        }
    }

    public String getMetadata(String href) throws IOException {
        String url = href + "/metadata";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/*+json;version=37.2")
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                // Debug logging for metadata response
                System.out.println("Metadata API call successful for: " + href);
                return responseBody;
            } else {
                System.err.println("Failed to retrieve metadata for VM " + href + " with code: " + response.code());
                if (response.body() != null) {
                    System.err.println("Error response: " + response.body().string());
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("Exception during metadata retrieval for " + href + ": " + e.getMessage());
            return null;
        }
    }
}