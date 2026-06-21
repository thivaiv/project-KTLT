package models;

import java.util.ArrayList;
import java.util.List;

public class CategoryNode {
    private String name;
    private String categoryType; // "THU", "CHI", "ROOT"
    private CategoryNode parent;
    private List<CategoryNode> children;
    private List<Transaction> transactions;
    private double budget; // biến lưu hạn mức

    private static final List<String> VALID_TYPES = List.of("THU", "CHI", "ROOT");

    public CategoryNode(String name, String categoryType, CategoryNode parent) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên danh mục không được để trống.");
        }
        if (!VALID_TYPES.contains(categoryType)) {
            throw new IllegalArgumentException("Loại danh mục không hợp lệ: '"
                    + categoryType + "'. Phải là một trong " + VALID_TYPES);
        }

        this.name = name.trim();
        this.categoryType = categoryType;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.transactions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        this.name = name.trim();
    }

    public String getCategoryType() {
        return categoryType;
    }

    public CategoryNode getParent() {
        return parent;
    }

    public void setParent(CategoryNode parent) {
        this.parent = parent;
    }

    public List<CategoryNode> getChildren() {
        return children;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public boolean addChild(CategoryNode childNode) {
        if (findChildByName(childNode.getName()) != null) {
            return false;
        }

        childNode.setParent(this);
        this.children.add(childNode);
        return true;
    }

    public CategoryNode removeChild(String name) {
        for (int i = 0; i < children.size(); i++) {
            CategoryNode child = children.get(i);
            if (child.getName().equals(name)) {
                CategoryNode removed = children.remove(i);
                removed.setParent(null); // Ngắt liên kết về cha
                return removed;
            }
        }
        return null;
    }

    public CategoryNode findChildByName(String name) {
        for (CategoryNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void addTransaction(Transaction txn) {
        txn.setCategoryPath(this.getPath());
        this.transactions.add(txn);
    }

    public double getDirectTotal() {
        double total = 0.0;
        for (Transaction txn : transactions) {
            total += txn.getAmount();
        }
        return total;
    }

    public String getPath() {
        if (parent == null) {
            // Nút ROOT — trả về "ROOT" (chỉ dùng nội bộ, không xuất ra CSV)
            return name;
        }
        if (parent.getParent() == null) {
            // Con trực tiếp của ROOT (là "THU" hoặc "CHI") — không kèm tiền tố ROOT/
            // Ví dụ: "THU", "CHI"
            return name;
        }
        // Mọi nút còn lại: "THU/Lương", "CHI/Nhu cầu thiết yếu/Ăn uống", ...
        return parent.getPath() + "/" + name;
    }

    public int getDepth() {
        if (parent == null) {
            return 0;
        }
        return 1 + parent.getDepth();
    }

    @Override
    public String toString() {
        return "CategoryNode(name='" + name + "', type='" + categoryType + "', children=" + children.size() + ", txns=" + transactions.size() + ")";
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        if (budget < 0) throw new IllegalArgumentException("Hạn mức không được âm.");
        this.budget = budget;
    }

}
