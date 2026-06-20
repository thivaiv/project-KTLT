package core;

import java.time.LocalDate;
import java.util.*;
import models.CategoryNode;
import models.Transaction;

public class FinanceTree {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String WHITE = "\u001B[38;5;15m";
    public static final String BG_RED = "\u001B[48;5;167m";
    public static final String DOLLAR = "\u001B[38;5;64m";

    private CategoryNode root;
    private Map<String, CategoryNode> nodeMap;

    public FinanceTree() {
        this.root = new CategoryNode("ROOT", "ROOT", null);
        this.nodeMap = new HashMap<>();
        initializeDefaultStructure();
    }

    public CategoryNode getRoot() {
        return root;
    }

    private void initializeDefaultStructure() {
        registerNode(root);

        // --- Nhánh THU (Chỉ giữ lại các mục cơ bản) ---
        CategoryNode thuNode = addNodeToParent("THU", "THU", root);
        addNodeToParent("Lương", "THU", thuNode);
        addNodeToParent("Thưởng", "THU", thuNode);
        addNodeToParent("Lãi tiết kiệm", "THU", thuNode);
        addNodeToParent("Thu nhập khác", "THU", thuNode);

        // --- Nhánh CHI (Cấu trúc phẳng, trực tiếp các mục cơ bản) ---
        CategoryNode chiNode = addNodeToParent("CHI", "CHI", root);
        addNodeToParent("Tiền nhà", "CHI", chiNode);
        addNodeToParent("Điện nước", "CHI", chiNode);
        addNodeToParent("Ăn uống", "CHI", chiNode);
        addNodeToParent("Đi lại", "CHI", chiNode);
        addNodeToParent("Mua sắm", "CHI", chiNode);
        addNodeToParent("Giải trí", "CHI", chiNode);
        addNodeToParent("Sức khỏe", "CHI", chiNode);
    }

    /** Hàm tiện ích: tạo nút, gán vào cha, đăng ký vào nodeMap. */
    private CategoryNode addNodeToParent(String name, String type, CategoryNode parent) {
        CategoryNode node = new CategoryNode(name, type, parent);
        parent.getChildren().add(node);
        registerNode(node);
        return node;
    }

    public void registerNode(CategoryNode node) {
        String path = node.getPath();
        nodeMap.put(path, node);
        for (Transaction txn : node.getTransactions()) {
            txn.setCategoryPath(path);
        }
    }

    public void unregisterSubtree(CategoryNode node) {
        String path = node.getPath();
        nodeMap.remove(path);
        for (CategoryNode child : node.getChildren()) {
            unregisterSubtree(child);
        }
    }

    public void reRegisterSubtree(CategoryNode node) {
        registerNode(node);
        for (CategoryNode child : node.getChildren()) {
            reRegisterSubtree(child);
        }
    }

    // =========================================================================
    // PHẦN 1: QUẢN LÝ NÚT CÂY (Tree Node Management)
    // =========================================================================

    public CategoryNode insertNode(String parentPath, String newName, String categoryType) {
        CategoryNode parent = getNodeByPath(parentPath);
        if (parent == null) {
            return null;
        }

        String actualType = categoryType;
        if (actualType == null || actualType.isEmpty()) {
            actualType = parent.getCategoryType();
            if (actualType.equals("ROOT")) {
                actualType = "CHI"; // Mặc định là CHI khi thêm vào gốc
            }
        }

        CategoryNode newNode = new CategoryNode(newName, actualType, parent);
        boolean success = parent.addChild(newNode);

        if (success) {
            registerNode(newNode);
            return newNode;
        }
        return null;
    }

    public boolean deleteNode(String nodePath, String mode) {
        CategoryNode node = getNodeByPath(nodePath);
        if (node == null) {
            return false;
        }

        if (node.getParent() == null || node.getName().equals("ROOT")) {
            // Không cho xóa nút gốc ROOT
            return false;
        }

        CategoryNode parent = node.getParent();

        if (mode.equalsIgnoreCase("CASCADE")) {
            // Xóa toàn bộ cây con khỏi bảng băm
            unregisterSubtree(node);
            // Xóa nút khỏi danh sách con của cha
            parent.removeChild(node.getName());
            return true;
        } else if (mode.equalsIgnoreCase("REPARENT")) {
            // Lưu danh sách con tạm thời
            List<CategoryNode> childrenToMove = new ArrayList<>(node.getChildren());
            for (CategoryNode child : childrenToMove) {
                // Hủy đăng ký cây con của child khỏi bảng băm dưới đường dẫn cũ
                unregisterSubtree(child);
                
                // Gán parent mới
                child.setParent(parent);
                parent.getChildren().add(child);
                
                // Đăng ký lại cây con của child dưới đường dẫn mới
                reRegisterSubtree(child);
            }
            
            // Chuyển giao dịch trực tiếp của nút bị xóa sang parent
            if (!node.getTransactions().isEmpty()) {
                parent.getTransactions().addAll(node.getTransactions());
                for (Transaction txn : node.getTransactions()) {
                    txn.setCategoryPath(parent.getPath());
                }
                node.getTransactions().clear();
            }
            
            // Xóa nút bị xóa khỏi children của parent
            parent.removeChild(node.getName());
            node.getChildren().clear();
            
            // Xóa nút bị xóa khỏi bảng băm
            nodeMap.remove(nodePath);
            
            return true;
        } else {
            return false;
        }
    }

    public boolean renameNode(String nodePath, String newName) {
        CategoryNode node = getNodeByPath(nodePath);
        if (node == null) {
            return false;
        }

        if (node.getParent() != null && node.getParent().findChildByName(newName) != null) {
            return false;
        }

        // Xóa các key cũ trong bảng băm
        unregisterSubtree(node);
        // Đổi tên
        node.setName(newName);
        // Đăng ký lại với key mới
        reRegisterSubtree(node);
        return true;
    }

    // =========================================================================
    // PHẦN 2: THUẬT TOÁN DFS - TÍNH TỔNG TIỀN THEO NHÁNH
    // =========================================================================

    public double calculateTotalDfs(CategoryNode node) {
        if (node == null) {
            node = this.root;
        }

        // Bước 1: Tính tổng giao dịch TRỰC TIẾP tại nút này
        double total = node.getDirectTotal();

        // Bước 2: ĐỆ QUY - Cộng thêm tổng từ các nút con
        for (CategoryNode child : node.getChildren()) {
            total += calculateTotalDfs(child);
        }

        return total;
    }

    public double[] calculateIncomeAndExpense() {
        // Sau khi fix getPath(), THU và CHI được lưu trong nodeMap với key "THU" và "CHI"
        CategoryNode incomeNode = getNodeByPath("THU");
        CategoryNode expenseNode = getNodeByPath("CHI");

        double totalIncome = incomeNode != null ? calculateTotalDfs(incomeNode) : 0.0;
        double totalExpense = expenseNode != null ? calculateTotalDfs(expenseNode) : 0.0;
        return new double[]{totalIncome, totalExpense};
    }

    // =========================================================================
    // PHẦN 3: TÌM KIẾM NODE BẰNG ĐƯỜNG DẪN
    // =========================================================================

    public CategoryNode getNodeByPath(String path) {
        return nodeMap.get(path);
    }

    public List<CategoryNode> searchByName(String name) {
        List<CategoryNode> results = new ArrayList<>();
        String nameLower = name.trim().toLowerCase();

        Queue<CategoryNode> queue = new LinkedList<>();
        queue.add(this.root);
        while (!queue.isEmpty()) {
            CategoryNode current = queue.poll();
            if (current.getName().toLowerCase().equals(nameLower)) {
                results.add(current);
            }
            queue.addAll(current.getChildren());
        }

        return results;
    }

    // =========================================================================
    // PHẦN 4: THUẬT TOÁN PHÂN LOẠI GIAO DỊCH (Transaction Classification)
    // =========================================================================

    public boolean classifyAndAddTransaction(Transaction txn, String categoryPath) {
        CategoryNode targetNode = getNodeByPath(categoryPath);

        if (targetNode == null) {
            // Thử tìm theo tên nếu không tìm được theo đường dẫn đầy đủ
            List<CategoryNode> results = searchByName(categoryPath);
            if (!results.isEmpty()) {
                targetNode = results.get(0);
            } else {
                return false;
            }
        }

        targetNode.addTransaction(txn);
        return true;
    }

    // =========================================================================
    // PHẦN 5: DUYỆT VÀ HIỂN THỊ CÂY (Tree Traversal & Display)
    // =========================================================================

    public String traverseTree(CategoryNode node, int indent) {
        StringBuilder sb = new StringBuilder();
        buildTreeVisualized(node, indent, sb);
        return sb.toString();
    }

    private void buildTreeVisualized(CategoryNode node, int indent, StringBuilder sb) {
        if (node == null) {
            node = this.root;
            sb.append("\n=======================================================\n");
            sb.append("  SƠ ĐỒ CÂY DANH MỤC TÀI CHÍNH\n");
            sb.append("=======================================================\n");
        }

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            prefix.append("    ");
        }


        double budget = node.getBudget();
        double total = calculateTotalDfs(node);
        int txnCount = node.getTransactions().size();
        String isLeafMarker = (node.isLeaf() && txnCount > 0) ? "*" : "";

        String info = "";
        String outBudget = "";
        if ((total > budget) & (budget>0) ){
            outBudget = RED + " quá hạn mức!" +RESET;
        }
        if (txnCount > 0) {
            if (budget > 0){
                info = String.format("%s" +DOLLAR+ "[%,.0f]" +RESET +BG_RED+ "|%,.0f|" +RESET+ " " +YELLOW+ "[%d GD]" +RESET, isLeafMarker,total,budget,txnCount);
            }
            else {
                info = String.format("%s" +DOLLAR+ "[%,.0f] " +RESET +YELLOW+ "[%d GD]" +RESET, isLeafMarker, total, txnCount);
            }
        } else if (total > 0) {
            if (budget > 0){
                info = String.format(DOLLAR+ "[%,.0f] " +RESET +BG_RED+ "|%,.0f|" +RESET, (total>0)?total:0,budget);
            }
            else {
                info = String.format(DOLLAR+ "[%,.0f]" +RESET, (total>0)?total:0);
            }
        }

        sb.append(prefix.toString()).append(indent > 0 ? "└── " : "").append("📁 ").append(node.getName()).append("  ").append(info).append(outBudget).append("\n");

        for (CategoryNode child : node.getChildren()) {
            buildTreeVisualized(child, indent + 1, sb);
        }
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> allTxns = new ArrayList<>();
        collectTransactionsRecursive(this.root, allTxns);
        // Sắp xếp theo ngày mới nhất (giảm dần)
        allTxns.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        return allTxns;
    }

    private void collectTransactionsRecursive(CategoryNode node, List<Transaction> result) {
        result.addAll(node.getTransactions());
        for (CategoryNode child : node.getChildren()) {
            collectTransactionsRecursive(child, result);
        }
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    public List<String> getAllLeafPaths() {
        List<String> paths = new ArrayList<>();
        getLeafPathsRecursive(this.root, paths);
        return paths;
    }

    private void getLeafPathsRecursive(CategoryNode node, List<String> paths) {
        if (node.isLeaf()) {
            paths.add(node.getPath());
        }
        for (CategoryNode child : node.getChildren()) {
            getLeafPathsRecursive(child, paths);
        }
    }
}
