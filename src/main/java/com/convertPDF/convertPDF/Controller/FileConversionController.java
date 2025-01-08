package com.convertPDF.convertPDF.Controller;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class FileConversionController {

    private static final String CONVERTED_FILES_DIR = "ConvertedFiles";

    // 📝 Endpoint para converter e salvar o arquivo com base no tipo de conversão
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> convertFile(@RequestParam("file") MultipartFile file, @RequestParam("conversionType") String conversionType) {
        Map<String, String> response = new HashMap<>();
        try {
            // Verifica se o arquivo foi enviado
            if (file.isEmpty()) {
                response.put("message", "No file provided.");
                return ResponseEntity.badRequest().body(response);
            }

            // Criação do diretório ConvertedFiles, se não existir
            Path directoryPath = Paths.get(CONVERTED_FILES_DIR);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            String filePrefix = conversionType.toLowerCase().contains("pdf") ? "pdf_" : "word_";
            String fileName = filePrefix + "converted_" + System.currentTimeMillis();
            Path outputPath;

            switch (conversionType.toLowerCase()) {
                case "wordtopdf":
                    fileName += ".pdf";
                    outputPath = directoryPath.resolve(fileName);
                    convertWordToPdf(file, outputPath);
                    break;
                case "pdftoword":
                    fileName += ".docx";
                    outputPath = directoryPath.resolve(fileName);
                    convertPdfToWord(file, outputPath);
                    break;
                default:
                    response.put("message", "Invalid conversion type.");
                    return ResponseEntity.badRequest().body(response);
            }

            response.put("message", "File successfully converted and saved at: " + outputPath.toAbsolutePath());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("message", "Error during file conversion.");
            return ResponseEntity.status(500).body(response);
        }
    }

    private void convertWordToPdf(MultipartFile file, Path outputPath) throws IOException {
        XWPFDocument document = new XWPFDocument(file.getInputStream());
        PdfWriter pdfWriter = new PdfWriter(Files.newOutputStream(outputPath));
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        Document pdfDoc = new Document(pdfDocument);

        document.getParagraphs().forEach(paragraph -> {
            pdfDoc.add(new Paragraph(paragraph.getText()));
        });

        pdfDoc.close();
        pdfDocument.close();
        document.close();
    }

    private void convertPdfToWord(MultipartFile file, Path outputPath) throws IOException {
        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String text = pdfStripper.getText(pdfDocument);

        XWPFDocument wordDocument = new XWPFDocument();
        String[] paragraphs = text.split("\n");
        for (String paragraph : paragraphs) {
            wordDocument.createParagraph().createRun().setText(paragraph);
        }

        try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
            wordDocument.write(out);
        }

        wordDocument.close();
        pdfDocument.close();
    }

    // 📂 Endpoint para listar arquivos na pasta ConvertedFiles
    @GetMapping("/files")
    public ResponseEntity<List<String>> listConvertedFiles() {
        try {
            Path directoryPath = Paths.get(CONVERTED_FILES_DIR);
            if (!Files.exists(directoryPath)) {
                return ResponseEntity.ok(List.of());
            }

            List<String> files = Files.list(directoryPath)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(files);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // 📥 Endpoint para baixar arquivos da pasta ConvertedFiles
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(CONVERTED_FILES_DIR).resolve(fileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
