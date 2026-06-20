package models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private static int idCounter = 0;
    
    private int transactionId;
    private double amount;
    private LocalDate date;
    private String note;
    private String categoryPath;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Transaction(double amount, LocalDate date, String note, String categoryPath) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền giao dịch phải lớn hơn 0. Nhận được: " + amount);
        }
        idCounter++;
        this.transactionId = idCounter;
        this.amount = amount;
        this.date = date;
        this.note = note != null ? note : "";
        this.categoryPath = categoryPath != null ? categoryPath : "";
    }

    public static void resetCounter() {
        idCounter = 0;
    }

    public static void setCounter(int value) {
        idCounter = value;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int id) {
        this.transactionId = id;
        if (id > idCounter) {
            idCounter = id;
        }
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public String getDetails() {
        return String.format("  [ID: %4d] Ngày: %s | Số tiền: %,12.0f VND | Danh mục: %-30s | Ghi chú: %s",
                transactionId,
                date.format(displayFormatter),
                amount,
                categoryPath,
                note);
    }

    public String toCsvRow() {
        String safeNote = note;
        if (safeNote.contains(",")) {
            safeNote = "\"" + safeNote.replace("\"", "\"\"") + "\"";
        }
        // Format SV1: YYYY-MM-DD,amount,CategoryPath,note
        return String.format(java.util.Locale.US, "%s,%.2f,%s,%s",
                date.format(formatter),
                amount,
                categoryPath,
                safeNote);
    }

    public static Transaction fromCsvRow(String row) {
        // Format SV1: YYYY-MM-DD,amount,CategoryPath,note
        String[] parts = row.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Dòng CSV không hợp lệ (cần ít nhất 3 cột): '" + row + "'");
        }

        LocalDate date = LocalDate.parse(parts[0].trim(), formatter);
        double amount = Double.parseDouble(parts[1].trim());
        String categoryPath = parts[2].trim();
        
        String note = "";
        if (parts.length > 3) {
            String noteField = parts[3].trim();
            if (noteField.startsWith("\"") && noteField.endsWith("\"")) {
                note = noteField.substring(1, noteField.length() - 1).replace("\"\"", "\"");
            } else {
                note = noteField.replace(";", ",");
            }
        }

        return new Transaction(amount, date, note, categoryPath);
    }

    @Override
    public String toString() {
        return "Transaction(id=" + transactionId + ", amount=" + amount + ", date=" + date + ", note='" + note + "')";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Transaction)) return false;
        Transaction other = (Transaction) obj;
        return this.transactionId == other.transactionId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(transactionId);
    }
}
