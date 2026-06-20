package core;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import models.CategoryNode;
import models.Transaction;

public class FinanceManager {
    public static final String DEFAULT_DATA_FILE = "data.csv";
    public static final String SEPARATOR = "---TREE---";

    private FinanceTree tree;
    private String dataFile;
    private final List<String> loadWarnings = new ArrayList<>();

    public List<String> getLoadWarnings() {
        return new ArrayList<>(loadWarnings);
    }

    public FinanceManager() {
        this(DEFAULT_DATA_FILE);
    }

    public FinanceManager(String dataFile) {
        this.tree = new FinanceTree();
        this.dataFile = dataFile;
    }

    public FinanceTree getTree() {
        return tree;
    }

    // =========================================================================
    // PHẦN 1: QUẢN LÝ DANH MỤC
    // =========================================================================

    public boolean addCategory(String parentPath, String name, String categoryType) {
        CategoryNode result = tree.insertNode(parentPath, name, categoryType);
        return result != null;
    }

    public boolean removeCategory(String path, String mode) {
        return tree.deleteNode(path, mode);
    }

    public String listCategories() {
        return tree.traverseTree(null, 0);
    }

    public List<String> listLeafCategories() {
        return tree.getAllLeafPaths();
    }

    // setBudget (test)
    public boolean setCategoryBudget(String categoryPath, double budget) {
        CategoryNode node = tree.getNodeByPath(categoryPath);
        if (node == null) {
            return false;
        }
        try {
            node.setBudget(budget);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // =========================================================================
    // PHẦN 2: QUẢN LÝ GIAO DỊCH
    // =========================================================================

    public boolean addTransaction(double amount, LocalDate date, String note, String categoryPath) {
        try {
            Transaction txn = new Transaction(amount, date, note, categoryPath);
            return tree.classifyAndAddTransaction(txn, categoryPath);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // =========================================================================
    // PHẦN 3: BÁO CÁO VÀ THỐNG KÊ
    // =========================================================================


    // =========================================================================
    /// XUẤT BÁO CÁO CHI TIẾT DANH MỤC CHI TIÊU RA CSV (e đội ơn Gemini ạ)
    // -------------------------------------------------------------------------

    /// XUẤT BÁO CÁO DẠNG TEXT BẢNG BIỂU RA CONSOLE (yêu Gemini)
    // =========================================================================
    public String generateDetailedTextReport() {
        StringBuilder sb = new StringBuilder();
        String hr1 = "+------------------------------------------+-----------------+-----------------+---------------------------------+\n";
        String hr2 = "+------------+------------------------------------------+-----------------+----------------------------------------+\n";
        String hr3 = "+------------------------------------------+-----------------+-----------------+\n";

        sb.append("\n==========================================================================================================\n");
        sb.append("                                    BÁO CÁO CHI TIẾT TÀI CHÍNH CÁ NHÂN\n");
        sb.append("                                      Thời gian: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        sb.append("==========================================================================================================\n\n");

        CategoryNode chiNode = null;
        for (CategoryNode child : tree.getRoot().getChildren()) {
            if ("CHI".equalsIgnoreCase(child.getName())) {
                chiNode = child;
                break;
            }
        }
        double totalChiSystem = calculateNodeTotalRecursive(chiNode);
        // ---------------------------------------------------------
        // TỶ TRỌNG CHI TIÊU CỦA CÁC MỤC LỚN
        // ---------------------------------------------------------
        sb.append("--- TỶ TRỌNG CHI TIÊU CÁC DANH MỤC LỚN ---\n");
        sb.append(hr3);
        sb.append(String.format("| %-40s | %-15s | %-15s |\n", "Danh mục", "Tổng chi (VND)", "Tỷ trọng (%)"));
        sb.append(hr3);

        if (chiNode != null && !chiNode.getChildren().isEmpty()) {
            for (CategoryNode subNode : chiNode.getChildren()) {
                double subTotal = calculateNodeTotalRecursive(subNode);
                double percentage = totalChiSystem > 0 ? (subTotal / totalChiSystem) * 100 : 0.0;

                sb.append(String.format("| %-40s | %,15.0f | %13.2f %% |\n",
                        truncateString(subNode.getName(), 40), subTotal, percentage));
            }
        } else {
            sb.append("| (Không có dữ liệu danh mục cấp 2 thuộc nhánh CHI)                                            |\n");
        }
        sb.append(hr3).append("\n");

        // ---------------------------------------------------------
        // BẢNG 1: TỔNG QUAN CHI TIÊU
        // ---------------------------------------------------------
        sb.append("--- [1] TỔNG QUAN CHI TIÊU ---\n");
        sb.append(hr1);
        sb.append(String.format("| %-40s | %-15s | %-15s | %-31s |\n", "Đường dẫn danh mục", "Đã chi (VND)", "Hạn mức (VND)", "Tình trạng"));
        sb.append(hr1);

        // Lấy tất cả các nút trong cây bằng cách duyệt đệ quy từ nút ROOT
        List<CategoryNode> allNodes = new ArrayList<>();
        collectAllNodesRecursive(tree.getRoot(), allNodes);

        for (CategoryNode node : allNodes) {
            // Bỏ qua nút ROOT, chỉ lấy các nhánh THU hoặc CHI (hoặc con của chúng)
            if (node.getParent() == null) continue;

            // Tính tổng tiền bao gồm cả nút con bằng hàm bổ trợ phía dưới
            double totalSpent = calculateNodeTotalRecursive(node);
            double budget = node.getBudget();
            String status = "-";

            if (budget > 0) {
                if (totalSpent > budget) {
                    status = String.format("!!..VƯỢT (%,.0f)", totalSpent - budget);
                } else {
                    status = String.format("Còn dư %,.0f", budget - totalSpent);
                }
            }

            String pathStr = truncateString(node.getPath(), 40);
            String budgetStr = budget > 0 ? String.format("%,.0f", budget) : "-";
            String statusStr = truncateString(status, 31);

            sb.append(String.format("| %-40s | %,15.0f | %15s | %-31s |\n", pathStr, totalSpent, budgetStr, statusStr));
        }
        sb.append(hr1);
        double totalThu = 0;
        double totalChi = 0;
        for (CategoryNode child : tree.getRoot().getChildren()) {
            if ("THU".equalsIgnoreCase(child.getName())) totalThu = calculateNodeTotalRecursive(child);
            if ("CHI".equalsIgnoreCase(child.getName())) totalChi = calculateNodeTotalRecursive(child);
        }
        double balance = totalThu - totalChi;
        sb.append(">>>>>>>>>>>>>>\n");
        sb.append(String.format(">>>>>>>>>>>>>> SỐ DƯ : %,.0f", balance));
        if (balance < 0){sb.append(String.format("......TIÊU NHIỀU HƠN KIẾM??"));}
        sb.append("\n");
        sb.append(">>>>>>>>>>>>>>\n");
        sb.append("\n\n");


        // ---------------------------------------------------------
        // BẢNG 2: LỊCH SỬ GIAO DỊCH
        // ---------------------------------------------------------
        sb.append("--- [2] LỊCH SỬ GIAO DỊCH ---\n");
        sb.append(hr2);
        sb.append(String.format("| %-10s | %-40s | %-15s | %-38s |\n", "Ngày", "Danh mục", "Số tiền (VND)", "Ghi chú"));
        sb.append(hr2);

        // Lấy toàn bộ giao dịch bằng phương thức có sẵn trong FinanceTree của bạn
        List<Transaction> allTransactions = tree.getAllTransactions();

        for (Transaction txn : allTransactions) {
            String dateStr = txn.getDate().toString();
            String pathStr = truncateString(txn.getCategoryPath(), 40);
            String noteStr = truncateString(txn.getNote(), 38);

            sb.append(String.format("| %-10s | %-40s | %,15.0f | %-38s |\n", dateStr, pathStr, txn.getAmount(), noteStr));
        }
        sb.append(hr2).append("\n");

        return sb.toString();
    }

    // Các hàm bổ trợ để thực hiện tính toán đúng cấu trúc cây
    private void collectAllNodesRecursive(CategoryNode node, List<CategoryNode> list) {
        if (node == null) return;
        list.add(node);
        for (CategoryNode child : node.getChildren()) {
            collectAllNodesRecursive(child, list);
        }
    }

    private double calculateNodeTotalRecursive(CategoryNode node) {
        double total = node.getDirectTotal(); // Hàm lấy tổng tiền giao dịch trực tiếp tại nút của bạn
        for (CategoryNode child : node.getChildren()) {
            total += calculateNodeTotalRecursive(child);
        }
        return total;
    }

    private String truncateString(String str, int length) {
        if (str == null) return "";
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    // Hàm thực hiện ghi chuỗi bảng trên ra file .txt
    public boolean exportDetailedReportToTxt(String fileName) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8")))) {
            writer.print(generateDetailedTextReport());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

//=======================================================================
//=======================================================================


    // =========================================================================
    // PHẦN 4: LƯU TRỮ DỮ LIỆU (File I/O)
    // =========================================================================

    public boolean saveData() {
        return saveData(this.dataFile);
    }

    public boolean saveData(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = this.dataFile;
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"))) {
            // -- Header --
            writer.write("# FinanceManager Data File - Saved: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("# Version: 1.0\n");

            // -- Phần 1: Cấu trúc cây --
            writer.write("# " + SEPARATOR + "\n");
            saveTreeStructure(writer, tree.getRoot());

            // -- Phần 2: Giao dịch --
            writer.write("---TRANSACTIONS---\n");
            List<Transaction> allTxns = tree.getAllTransactions();
            // Sắp xếp theo ID tăng dần khi lưu
            allTxns.sort((t1, t2) -> Integer.compare(t1.getTransactionId(), t2.getTransactionId()));
            for (Transaction txn : allTxns) {
                writer.write(txn.toCsvRow() + "\n");
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    private void saveTreeStructure(BufferedWriter writer, CategoryNode node) throws IOException {
        if (!node.getName().equals("ROOT")) {
            String path = node.getPath();
            // Bắt buộc dùng Locale.US để tránh việc Java tự chèn dấu phẩy (,) gây lỗi cấu trúc file CSV.
            String budgetStr = String.format(java.util.Locale.US, "%.0f", node.getBudget());
            writer.write(path + "," + node.getCategoryType() + "," + budgetStr + "\n");
        }
        for (CategoryNode child : node.getChildren()) {
            saveTreeStructure(writer, child);
        }
    }

    public boolean loadData() {
        return loadData(this.dataFile);
    }

    public boolean loadData(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = this.dataFile;
        }

        loadWarnings.clear();
        File file = new File(filename);
        if (!file.exists()) {
            loadWarnings.add(String.format("File '%s' chưa có, bắt đầu mới.", filename));
            return false;
        }

        this.tree = new FinanceTree();
        Transaction.resetCounter();
        boolean inTransactionsSection = false;
        int nodesCreated = 0;
        int txnsLoaded = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                // Bỏ qua dòng comment và dòng trống
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("---TRANSACTIONS---")) {
                    inTransactionsSection = true;
                    continue;
                }

                if (!inTransactionsSection) {
                    // -- Xử lý cấu trúc cây --
                    ///  Format: "Root/Chi tiêu/Ăn uống,Expense"
                    ///        Đọc " Root/Cata/hạn mức"
                    try {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String path = parts[0].trim();
                            String catType = parts[1].trim();
                            double budget = 0.0;

                            // Nếu file có chứa cột hạn mức (file mới)
                            if (parts.length >= 3) {
                                try {
                                    budget = Double.parseDouble(parts[2].trim());
                                } catch (NumberFormatException ignored) {
                                }
                            }

                            if (!path.equals("THU") && !path.equals("CHI") && !path.startsWith("THU/") && !path.startsWith("CHI/")) {
                                loadWarnings.add(String.format("Line %d: invalid path '%s' (phải bắt đầu bằng THU/ hoặc CHI/)", lineNum, path));
                                continue;
                            }

                            CategoryNode existingNode = tree.getNodeByPath(path);

                            if (existingNode == null) {
                                int lastSlashIdx = path.lastIndexOf('/');
                                String parentPath = lastSlashIdx != -1 ? path.substring(0, lastSlashIdx) : "ROOT";
                                String nodeName = lastSlashIdx != -1 ? path.substring(lastSlashIdx + 1) : path;

                                CategoryNode createdNode = tree.insertNode(parentPath, nodeName, catType);
                                if (createdNode != null) {
                                    createdNode.setBudget(budget); // Gán hạn mức cho nút mới
                                    nodesCreated++;
                                }
                            } else {
                                // BỔ SUNG PHẦN NÀY: Nếu nút đã tồn tại (như nút CHI mặc định), nạp lại hạn mức từ file CSV
                                existingNode.setBudget(budget);
                            }
                        } else {
                            loadWarnings.add(String.format("Line %d: invalid path format", lineNum));
                        }
                    } catch (Exception e) {
                        loadWarnings.add(String.format("Lỗi dòng %d (cây): %s", lineNum, e.getMessage()));
                    }

                } else {
                    // -- Xử lý giao dịch --
                    try {
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 4);
                        if (parts.length < 4) {
                            loadWarnings.add(String.format("Line %d: invalid columns count", lineNum));
                            continue;
                        }

                        LocalDate date;
                        try {
                            date = LocalDate.parse(parts[0].trim(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        } catch (java.time.format.DateTimeParseException e) {
                            loadWarnings.add(String.format("Line %d: invalid date format", lineNum));
                            continue;
                        }

                        double amount;
                        try {
                            amount = Double.parseDouble(parts[1].trim());
                        } catch (NumberFormatException e) {
                            loadWarnings.add(String.format("Line %d: invalid amount format", lineNum));
                            continue;
                        }

                        if (amount <= 0) {
                            loadWarnings.add(String.format("Line %d: invalid amount <= 0", lineNum));
                            continue;
                        }

                        String categoryPath = parts[2].trim();
                        if (!categoryPath.contains("/")) {
                            loadWarnings.add(String.format("Line %d: invalid path", lineNum));
                            continue;
                        }

                        if (!categoryPath.startsWith("THU/") && !categoryPath.startsWith("CHI/")) {
                            loadWarnings.add(String.format("Line %d: invalid path prefix", lineNum));
                            continue;
                        }

                        String noteField = parts[3].trim();
                        String note = "";
                        if (noteField.startsWith("\"") && noteField.endsWith("\"")) {
                            note = noteField.substring(1, noteField.length() - 1).replace("\"\"", "\"");
                        } else {
                            note = noteField.replace(";", ",");
                        }

                        Transaction txn = new Transaction(amount, date, note, categoryPath);
                        boolean added = tree.classifyAndAddTransaction(txn, categoryPath);
                        if (added) {
                            txnsLoaded++;
                        } else {
                            loadWarnings.add(String.format("Line %d: could not classify transaction into '%s'", lineNum, categoryPath));
                        }
                    } catch (Exception e) {
                        loadWarnings.add(String.format("Lỗi dòng %d (giao dịch): %s", lineNum, e.getMessage()));
                    }
                }
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

}
