package chunks.oci;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.regex.Pattern;

/** Lists OCI objects eligible for the project's lifecycle policy. */
public class OCIDeletableObjectLister {
    private static final Pattern OPERATION_PATTERN = Pattern.compile("^inbox/[^/]+/op_.*");
    private static final Pattern DATA_FILE_PATTERN = Pattern.compile("^inbox/[^/]+/data-file/.*");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter OCI profile name (from ~/.oci/config): ");
        String profile = scanner.nextLine().trim();
        System.out.print("Enter namespace: ");
        String namespace = scanner.nextLine().trim();
        System.out.print("Enter bucket name: ");
        String bucket = scanner.nextLine().trim();

        try {
            ConfigFileAuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(profile);
            ObjectStorage client = new ObjectStorageClient(provider);
            String startToken = null;
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

            do {
                ListObjectsResponse response = client.listObjects(ListObjectsRequest.builder()
                        .namespaceName(namespace).bucketName(bucket).limit(1000).page(startToken).build());
                for (ObjectSummary object : response.getListObjects().getObjects()) {
                    String name = object.getName();
                    if (OPERATION_PATTERN.matcher(name).matches() || DATA_FILE_PATTERN.matcher(name).matches()) {
                        ZonedDateTime created = object.getTimeCreated().toInstant().atZone(ZoneOffset.UTC);
                        if (Duration.between(created, now).toDays() >= 1) {
                            System.out.printf("%-60s Created: %s | Age: %d days%n", name, created,
                                    Duration.between(created, now).toDays());
                        }
                    }
                }
                startToken = response.getListObjects().getNextStartWith();
            } while (startToken != null && !startToken.isEmpty());
            client.close();
        } catch (IOException e) {
            System.err.println("Failed to load OCI config: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("OCI error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
