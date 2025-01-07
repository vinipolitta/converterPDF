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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class FileConversionController {

    private static final String CONVERTED_FILES_DIR = "ConvertedFiles";

    // 📝 Endpoint para converter e salvar o arquivo Word como PDF
    @PostMapping(value = "/word-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> convertWordToPdf(@RequestParam("file") MultipartFile file) {
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

            // Nome do arquivo PDF
            String fileName = "converted_" + System.currentTimeMillis() + ".pdf";
            Path pdfPath = directoryPath.resolve(fileName);

            // Converte Word para PDF e salva no diretório
            XWPFDocument document = new XWPFDocument(file.getInputStream());
            PdfWriter pdfWriter = new PdfWriter(Files.newOutputStream(pdfPath));
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document pdfDoc = new Document(pdfDocument);

            document.getParagraphs().forEach(paragraph -> {
                pdfDoc.add(new Paragraph(paragraph.getText()));
            });

            pdfDoc.close();
            pdfDocument.close();
            document.close();

            response.put("message", "File successfully converted and saved at: " + pdfPath.toAbsolutePath());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("message", "Error during file conversion.");
            return ResponseEntity.status(500).body(response);
        }
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
