package com.lnmcp.lena.service.impl;

import com.lnmcp.lena.model.DocumentContext;
import com.lnmcp.lena.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentService for processing PDF and PPT documents.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    @Value("${mcp.documents.path}")
    private String documentsPath;

    @Override
    public DocumentContext extractContext(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString().toLowerCase();

        if (filename.endsWith(".pdf")) {
            return extractPdfContext(filePath);
        } else if (filename.endsWith(".ppt") || filename.endsWith(".pptx")) {
            return extractPptContext(filePath);
        } else {
            throw new IOException("Unsupported file format: " + filename);
        }
    }

    @Override
    public List<DocumentContext> extractContextFromMultipleDocuments(List<Path> filePaths) throws IOException {
        List<DocumentContext> contexts = new ArrayList<>();

        for (Path filePath : filePaths) {
            try {
                contexts.add(extractContext(filePath));
            } catch (IOException e) {
                log.error("Error extracting context from file: {}", filePath, e);
                throw e;
            }
        }

        return contexts;
    }

    @Override
    public DocumentContext extractContextByFilename(String filename) throws IOException {
        Path filePath = Paths.get(documentsPath, filename);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filename);
        }
        return extractContext(filePath);
    }

    @Override
    public List<DocumentContext> extractContextFromMultipleDocumentsByFilename(List<String> filenames) throws IOException {
        List<Path> filePaths = filenames.stream()
                .map(filename -> Paths.get(documentsPath, filename))
                .collect(Collectors.toList());

        return extractContextFromMultipleDocuments(filePaths);
    }

    /**
     * Extract context from a PDF file
     */
    private DocumentContext extractPdfContext(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return DocumentContext.builder()
                    .filename(filePath.getFileName().toString())
                    .documentType(DocumentContext.DocumentType.PDF)
                    .content(text)
                    .pageNumber(document.getNumberOfPages()) // Total pages
                    .build();
        }
    }

    /**
     * Extract context from a PPT file
     */
    private DocumentContext extractPptContext(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString().toLowerCase();
        StringBuilder content = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            for (XSLFSlide slide : ppt.getSlides()) {
                // Try to get slide title if available
                String title = null;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        if (textShape.getText() != null && !textShape.getText().isEmpty()) {
                            if (title == null) {
                                title = textShape.getText(); // Use first text shape as title
                                content.append("Slide Title: ").append(title).append("\n");
                            } else {
                                content.append("Content: ").append(textShape.getText()).append("\n");
                            }
                        }
                    }
                }
                content.append("\n");
            }

            return DocumentContext.builder()
                    .filename(filename)
                    .documentType(DocumentContext.DocumentType.PPT)
                    .content(content.toString())
                    .pageNumber(ppt.getSlides().size()) // Total slides
                    .build();
        }
    }
}
