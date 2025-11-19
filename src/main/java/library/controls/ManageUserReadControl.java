package library.controls;

import library.models.Book;
import library.models.Borrow;
import library.models.User;
import library.persistence.Repository;
import library.utils.HasMessage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;

public record ManageUserReadControl(Repository repository) {
    @NotNull
    public ReadResult readBook(User user, Book book) {
        Optional<Borrow> borrowData = repository.borrowOps.read(user, book);
        Optional<Book.Data> bookData = repository.bookOps.read(book);
        if (borrowData.isEmpty()) return new ReadResult.BorrowDataNotFound();
        if (bookData.isEmpty()) return new ReadResult.BookDataNotFound();

        // Try to access a pre-existing PDF file; generate one if none exists
        String pdfPath = borrowData.get().pdfPath();
        File f = new File(pdfPath);
        if (!f.exists()) {
            boolean pdfGenerationResult = generatePdf(bookData.get().content(), pdfPath);
            return pdfGenerationResult ?
                    new ReadResult.NewPdfGenerated(pdfPath) :
                    new ReadResult.PdfGenerationError();
        } else return new ReadResult.Success(pdfPath);
    }

    private boolean generatePdf(String content, String pdfPath) {
        try {
            Document outputDoc = new Document(PageSize.A4, 50, 50, 50, 50);
            FileOutputStream os = new FileOutputStream(pdfPath);
            PdfWriter.getInstance(outputDoc, os);
            outputDoc.open();
            for (String line: content.split("\\r?\\n")) {
                Paragraph p = new Paragraph(line);
                p.setAlignment(Element.ALIGN_JUSTIFIED);
                outputDoc.add(p);
            }
            outputDoc.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public sealed interface ReadResult {
        record Success(@Getter String path) implements ReadResult {
        }

        record NewPdfGenerated(@Getter String path) implements ReadResult {
        }

        record PdfGenerationError() implements ReadResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return "Error while generating PDF";
            }
        }

        record BookDataNotFound() implements ReadResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return "The data for the book is not found";
            }
        }

        record BorrowDataNotFound() implements ReadResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return "The borrow data for the book is not found";
            }
        }

        record BookExpired() implements ReadResult, HasMessage {
            @Override
            public @NotNull String getMessage() {
                return "The borrow period for the book has expired";
            }
        }
    }
}
