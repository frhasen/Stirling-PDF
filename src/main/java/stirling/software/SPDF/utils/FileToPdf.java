package stirling.software.SPDF.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import stirling.software.SPDF.utils.ProcessExecutor.ProcessExecutorResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileToPdf {
    public static byte[] convertHtmlToPdf(byte[] fileBytes, String fileName) throws IOException, InterruptedException {

        Path tempOutputFile = Files.createTempFile("output_", ".pdf");
        Path tempInputFile = null;
        byte[] pdfBytes;
        try {
            if (fileName.endsWith(".html")) {
                tempInputFile = Files.createTempFile("input_", ".html");
                Files.write(tempInputFile, fileBytes);
            } else {
                tempInputFile = unzipAndGetMainHtml(fileBytes);
            }

            List<String> command = new ArrayList<>();
            command.add("weasyprint");
            command.add(tempInputFile.toString());
            command.add(tempOutputFile.toString());
            ProcessExecutorResult returnCode;
            if (fileName.endsWith(".zip")) {
                returnCode = ProcessExecutor.getInstance(ProcessExecutor.Processes.WEASYPRINT)
                        .runCommandWithOutputHandling(command, tempInputFile.getParent().toFile());
            } else {

                returnCode = ProcessExecutor.getInstance(ProcessExecutor.Processes.WEASYPRINT)
                        .runCommandWithOutputHandling(command);
            }

            pdfBytes = Files.readAllBytes(tempOutputFile);
        } finally {
            // Clean up temporary files
            Files.delete(tempOutputFile);
            Files.delete(tempInputFile);

            if (fileName.endsWith(".zip")) {
                GeneralUtils.deleteDirectory(tempInputFile.getParent());
            }
        }

        return pdfBytes;
    }


    private static Path unzipAndGetMainHtml(byte[] fileBytes) throws IOException {
        Path tempDirectory = Files.createTempDirectory("unzipped_");
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = tempDirectory.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);  // Explicitly create the directory structure
                } else {
                    Files.createDirectories(filePath.getParent()); // Create parent directories if they don't exist
                    Files.copy(zipIn, filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        //search for the main HTML file.
        try (Stream<Path> walk = Files.walk(tempDirectory)) {
            List<Path> htmlFiles = walk.filter(file -> file.toString().endsWith(".html"))
                    .collect(Collectors.toList());

            if (htmlFiles.isEmpty()) {
                throw new IOException("No HTML files found in the unzipped directory.");
            }

            // Prioritize 'index.html' if it exists, otherwise use the first .html file
            for (Path htmlFile : htmlFiles) {
                if (htmlFile.getFileName().toString().equals("index.html")) {
                    return htmlFile;
                }
            }

            return htmlFiles.get(0);
        }
    }

    public static byte[] convertHtmlToFormPdf(byte[] fileBytes, String fileName) throws IOException, InterruptedException {
        String html = new String(fileBytes);
        // parse html document
        Document document =
                Jsoup.parse(html);
        // find all radio buttons
        Elements inputs = document.select("input[type='radio']");
        int i = 0; // create a counter to distinguish the names of the radio buttons to make them selectable on by one
        for (var input : inputs) {
            // change type
            input.attr("type", "checkbox");
            // change name
            String name = input.attr("name");
            input.addClass("radio-btn");
            input.attr("name", String.format("%s-%d", name, i++));
        }
        // dom back to string
        html = document.html();
        fileBytes = html.getBytes();

        Path tempOutputFile = Files.createTempFile("output_", ".pdf");
        Path tempInputFile = null;
        byte[] pdfBytes;
        try {
            if (fileName.endsWith(".html")) {
                tempInputFile = Files.createTempFile("input_", ".html");
                Files.write(tempInputFile, fileBytes);
            } else {
                tempInputFile = unzipAndGetMainHtml(fileBytes);
            }

            List<String> command = new ArrayList<>();
            command.add("weasyprint");
            command.add("--pdf-forms");
            command.add("--full-fonts");
            command.add("--pdf-variant");
            command.add("pdf/a-3b");
            command.add("--pdf-version");
            command.add("2.0");
            command.add(tempInputFile.toString());
            command.add(tempOutputFile.toString());
            ProcessExecutorResult returnCode;
            if (fileName.endsWith(".zip")) {
                returnCode = ProcessExecutor.getInstance(ProcessExecutor.Processes.WEASYPRINT)
                        .runCommandWithOutputHandling(command, tempInputFile.getParent().toFile());
            } else {

                returnCode = ProcessExecutor.getInstance(ProcessExecutor.Processes.WEASYPRINT)
                        .runCommandWithOutputHandling(command);
            }

            pdfBytes = Files.readAllBytes(tempOutputFile);
        } finally {
            // Clean up temporary files
            Files.delete(tempOutputFile);
            Files.delete(tempInputFile);

            if (fileName.endsWith(".zip")) {
                GeneralUtils.deleteDirectory(tempInputFile.getParent());
            }
        }

        return pdfBytes;
    }
}
