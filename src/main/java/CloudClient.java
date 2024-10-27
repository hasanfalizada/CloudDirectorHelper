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
                return response.body().string();
            } else {
                System.err.println("Failed to retrieve metadata for VM " + href + " with code: " + response.code());
                return null;
            }
        }
    }


}
