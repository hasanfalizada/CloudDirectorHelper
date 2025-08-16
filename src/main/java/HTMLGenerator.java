import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class HTMLGenerator {
    public void generateHTML(List<VMInfo> vms, String directoryPath) {
        // Pass directoryPath to Main for debug logging
        Main.setDebugDirectory(directoryPath);

        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
                .append("<style>")
                .append("table { border-collapse: collapse; width: 100%; }")
                .append("th, td { border: 1px solid black; padding: 8px; text-align: left; }")
                .append("</style>")
                .append("</head><body><h1>VM List</h1><table>");

        html.append("<tr>")
                .append("<th>Row Number</th>")
                .append("<th>Name</th>")
                .append("<th>Guest OS</th>")
                .append("<th>CPUs</th>")
                .append("<th>Memory (MB)</th>")
                .append("<th>Status</th>")
                .append("<th>Network</th>")
                .append("<th>IP Address</th>")
                .append("<th>Date Created</th>")
                .append("<th>Total Storage (MB)</th>")
                .append("<th>Project</th>")
                .append("<th>Owner</th>")
                .append("<th>Environment</th>")
                .append("<th>Severity</th>")
                .append("<th>Purpose</th>")
                .append("</tr>");

        int rowNumber = 1; // Initialize row number counter
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // Define output date format

        for (VMInfo vm : vms) {
            Map<String, String> metadata = vm.getMetadata();
            String dateString = vm.getDateCreated(); // Assuming this returns the timestamp as a string
            String formattedDate = "";
            try {
                Date createdDate = inputFormat.parse(dateString.replace("+0400", "+0400")); // Handle timezone if necessary
                formattedDate = outputFormat.format(createdDate);
            } catch (ParseException e) {
                e.printStackTrace(); // Handle parsing exception
            }

            html.append("<tr>")
                    .append("<td>").append(rowNumber++).append("</td>") // Add row number
                    .append("<td>").append(vm.getName()).append("</td>")
                    .append("<td>").append(vm.getGuestOs()).append("</td>")
                    .append("<td>").append(String.format("%,d", vm.getNumberOfCpus())).append("</td>") // Format CPUs
                    .append("<td>").append(String.format("%,d", vm.getMemoryMB())).append("</td>") // Format Memory
                    .append("<td>").append(vm.getStatus()).append("</td>")
                    .append("<td>").append(vm.getNetworkName()).append("</td>")
                    .append("<td>").append(vm.getIpAddress()).append("</td>")
                    .append("<td>").append(formattedDate).append("</td>") // Use formatted date
                    .append("<td>").append(String.format("%,d", vm.getTotalStorageAllocatedMb())).append("</td>") // Format Total Storage
                    .append("<td>").append(metadata.getOrDefault("Project", "")).append("</td>")
                    .append("<td>").append(metadata.getOrDefault("Owner", "")).append("</td>")
                    .append("<td>").append(metadata.getOrDefault("Environment", "")).append("</td>")
                    .append("<td>").append(metadata.getOrDefault("Severity", "")).append("</td>")
                    .append("<td>").append(metadata.getOrDefault("Purpose", "")).append("</td>")
                    .append("</tr>");
        }

        html.append("</table></body></html>");

        String filePath = directoryPath + "\\VMReport.html";

        // Write HTML file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(html.toString());
            System.out.println("HTML file generated on " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to write HTML file: " + e.getMessage());
        }

        // Generate Excel file
        generateExcel(vms, directoryPath);
    }

    private void generateExcel(List<VMInfo> vms, String directoryPath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("VM List");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Row Number", "Name", "Guest OS", "CPUs", "Memory (MB)", "Status", "Network", "IP Address", "Date Created", "Total Storage (MB)", "Project", "Owner", "Environment", "Severity", "Purpose"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        int rowNumber = 1; // Initialize row number counter
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        for (VMInfo vm : vms) {
            Row row = sheet.createRow(rowNumber++);
            Map<String, String> metadata = vm.getMetadata();
            String dateString = vm.getDateCreated(); // Assuming this returns the timestamp as a string
            String formattedDate = "";
            try {
                Date createdDate = inputFormat.parse(dateString.replace("+0400", "+0400")); // Handle timezone if necessary
                formattedDate = outputFormat.format(createdDate);
            } catch (ParseException e) {
                e.printStackTrace(); // Handle parsing exception
            }

            row.createCell(0).setCellValue(rowNumber - 1); // Row number
            row.createCell(1).setCellValue(vm.getName());
            row.createCell(2).setCellValue(vm.getGuestOs());
            row.createCell(3).setCellValue(vm.getNumberOfCpus());
            row.createCell(4).setCellValue(vm.getMemoryMB());
            row.createCell(5).setCellValue(vm.getStatus());
            row.createCell(6).setCellValue(vm.getNetworkName());
            row.createCell(7).setCellValue(vm.getIpAddress());
            row.createCell(8).setCellValue(formattedDate); // Use formatted date
            row.createCell(9).setCellValue(vm.getTotalStorageAllocatedMb());
            row.createCell(10).setCellValue(metadata.getOrDefault("Project", ""));
            row.createCell(11).setCellValue(metadata.getOrDefault("Owner", ""));
            row.createCell(12).setCellValue(metadata.getOrDefault("Environment", ""));
            row.createCell(13).setCellValue(metadata.getOrDefault("Severity", ""));
            row.createCell(14).setCellValue(metadata.getOrDefault("Purpose", ""));
        }

        String filePath = directoryPath + "\\VMReport.xlsx";
        // Write Excel file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
            System.out.println("Excel file generated on " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to write Excel file: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                System.err.println("Failed to close workbook: " + e.getMessage());
            }
        }
    }
}