import core.FinanceManager;
import models.CategoryNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ConsoleMenu {
    private final FinanceManager manager;
    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ConsoleMenu() {
        this(new FinanceManager());
    }

    public ConsoleMenu(FinanceManager manager) {
        this.manager = manager;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        // Tự động tải dữ liệu khi khởi chạy
        manager.loadData();
        for (String warning : manager.getLoadWarnings()) {
            System.out.println("  [CẢNH BÁO] " + warning);
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = readLine().trim();
            switch (choice) {
                case "1":
                    manageCategoriesMenu();
                    break;
                case "2":
                    addTransactionMenu();
                    break;
                case "3":
                    report();
                    waitEnter();
                    break;
                //========================================
                case "4":
                    saveLoadMenu();
                    break;
                case "5":
                    System.out.println("\n  [HỆ THỐNG] Đang tự động lưu dữ liệu trước khi thoát...");
                    manager.saveData();
                    System.out.println("  [OK] Đã thoát chương trình. Tạm biệt!");
                    running = false;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ, vui lòng chọn lại (1-5).");
                    waitEnter();
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           HỆ THỐNG QUẢN LÝ TÀI CHÍNH CÁ NHÂN             ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  1. 📁 Quản lý Danh mục (Thêm / Xóa / Đổi tên)           ║");
        System.out.println("║  2. 📝 Nhập liệu Giao dịch mới                           ║");
        System.out.println("║  3. 📊 Xem Báo cáo Tổng quan Tài chính                   ║");
        System.out.println("║  4. 💾 Lưu trữ & Tải lại dữ liệu (CSV)                   ║");
        System.out.println("║  5. ❌ Thoát chương trình & Tự động lưu                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.print("  👉 Nhập lựa chọn của bạn (1-5): ");
    }

    // =========================================================================
    // 1. QUẢN LÝ DANH MỤC
    // =========================================================================
    private void manageCategoriesMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   📁 QUẢN LÝ DANH MỤC                    ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  1.1 Xem cấu trúc cây danh mục hiện tại                  ║");
            System.out.println("║  1.2 Thêm danh mục mới                                   ║");
            System.out.println("║  1.3 Xóa danh mục (CASCADE / REPARENT)                   ║");
            System.out.println("║  1.4 Đặt hạn mức chi tiêu (Budget)                       ║");
            System.out.println("║  1.5 Quay lại Menu chính                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.print("  👉 Nhập lựa chọn của bạn (1.1-1.5): ");
            String choice = readLine().trim();
            switch (choice) {
                case "1.1":
                case "1":
                    System.out.print(manager.listCategories());
                    waitEnter();
                    break;
                case "1.2":
                case "2":
                    addCategoryFlow();
                    break;
                case "1.3":
                case "3":
                    deleteCategoryFlow();
                    break;
                case "1.4":
                case "4":
                    setBudgetFlow();
                    break;
                case "1.5":
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ.");
                    waitEnter();
            }
        }
    }

    private void addCategoryFlow() {
        System.out.println("\n  --- THÊM DANH MỤC MỚI ---");
        System.out.print("  Nhập đường dẫn danh mục cha (ví dụ: 'THU' hoặc 'CHI/Nhu cầu thiết yếu'): ");
        String parentPath = readLine().trim();
        System.out.print("  Nhập tên danh mục mới muốn tạo: ");
        String name = readLine().trim();
        System.out.print("  Nhập loại danh mục (THU / CHI, để trống để tự động kế thừa từ cha): ");
        String typeInput = readLine().trim().toUpperCase();

        if (name.isEmpty()) {
            System.out.println("  [LỖI] Tên danh mục không được để trống.");
            waitEnter();
            return;
        }

        String type = typeInput.isEmpty() ? null : typeInput;
        boolean success = manager.addCategory(parentPath, name, type);
        if (success) {
            System.out.printf("  [OK] Đã thêm thành công danh mục '%s' vào '%s'.\n", name, parentPath);
        } else {
            System.out.println("  [LỖI] Thêm danh mục thất bại. Vui lòng kiểm tra lại đường dẫn cha hoặc tính trùng lặp tên.");
        }
        waitEnter();
    }

    private void deleteCategoryFlow() {
        System.out.println("\n  --- XÓA DANH MỤC ---");
        String path = pickMenu();

        if (path == null){
            System.out.println("  [LỖI] đường dẫn không tồn tại.");
            waitEnter();
            return;
        }
        if (path.isEmpty() || path.equals("THU") || path.equals("CHI") || path.equals("ROOT")) {
            System.out.println("  [LỖI] Không được phép xóa nút ROOT hoặc các nhánh gốc mặc định (THU / CHI).");
            waitEnter();
            return;
        }

        System.out.println("  Chọn chế độ xóa:");
        System.out.println("     1. CASCADE : Xóa danh mục này và toàn bộ danh mục con + giao dịch của nó.");
        System.out.println("     2. REPARENT: Xóa danh mục này, chuyển các con và giao dịch trực tiếp lên cha.");
        System.out.print("  Nhập chế độ (1 hoặc 2): ");
        String mode = readLine().trim();
        if (!mode.equals("CASCADE") && !mode.equals("REPARENT")) {
            switch (mode){
                case "1":
                    mode = "CASCADE";
                    break;
                case "2":
                    mode = "REPARENT";
                    break;
                default:
                    mode = "bruh";
                    break;
            }
        }
        if (!mode.equals("CASCADE") && !mode.equals("REPARENT")) {
            System.out.println("  [LỖI] Chế độ xóa không hợp lệ.");
            waitEnter();
            return;
        }

        System.out.printf("  [WARNING] Bạn có chắc chắn muốn xóa danh mục '%s' với chế độ '%s'? (y/n): ", path, mode);
        String confirm = readLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("  [HỦY] Hủy bỏ thao tác xóa.");
            waitEnter();
            return;
        }

        boolean success = manager.removeCategory(path, mode);
        if (success) {
            System.out.println("  [OK] Đã xóa danh mục thành công.");
        } else {
            System.out.println("  [LỖI] Xóa danh mục thất bại (đường dẫn không tồn tại hoặc lỗi trong quá trình xử lý).");
        }
        waitEnter();
    }


    // setBudget (test)
    private void setBudgetFlow(){
        System.out.println("\n  --- ĐẶT HẠN MỨC CHI TIÊU ---");

        String categoryPath = CHIpickMenu();

        System.out.print("  Nhập số tiền hạn mức (VND): ");
        double budget;
        try {
            budget = Double.parseDouble(readLine().trim());
            if (budget < 0) {
                System.out.println("  [LỖI] Hạn mức không được là số âm.");
                waitEnter();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("  [LỖI] Số tiền nhập vào không hợp lệ.");
            waitEnter();
            return;
        }

        boolean success = manager.setCategoryBudget(categoryPath, budget);
        if (success) {
            System.out.printf("  [OK] Đã đặt hạn mức %,.0f VND cho danh mục '%s'.\n", budget, categoryPath);
        } else {
            System.out.println("  [LỖI] Đặt hạn mức thất bại. Vui lòng kiểm tra lại đường dẫn danh mục (đảm bảo danh mục tồn tại).");
        }
        waitEnter();
    }

    // MENU chọn danh mục cho các thao tác trên
    /// Menu chỉ có CHI cho setnudget
    public String CHIpickMenu() {
        CategoryNode chiRoot = manager.getTree().getRoot().getChildren().stream()
                .filter(n -> "CHI".equals(n.getName()))
                .findFirst().orElse(null);

        if (chiRoot == null) {
            System.out.println("  [LỖI] Không tìm thấy dữ liệu nhánh CHI.");
            return null;
        }

        List<CategoryNode> listNodes = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("\n  === CHỌN DANH MỤC CHI ===\n");

        // Gọi hàm đệ quy để dựng sơ đồ cây và nạp vào danh sách
        buildTreeForSelection(chiRoot, 0, listNodes, sb);
        System.out.print(sb.toString());

        System.out.print("\n  Nhập số thứ tự danh mục để chọn (Để trống để hủy): ");
        String inputIndex = readLine().trim();
        if (inputIndex.isEmpty()) {
            return null;
        }

        try {
            int index = Integer.parseInt(inputIndex) - 1; // Convert về index 0-based
            if (index >= 0 && index < listNodes.size()) {
                CategoryNode selectedNode = listNodes.get(index);
                return selectedNode.getPath(); // Chỉ trả về đường dẫn
            } else {
                System.out.println("  [LỖI] Số thứ tự lựa chọn không nằm trong danh sách.");
            }
        } catch (NumberFormatException e) {
            System.out.println("  [LỖI] Vui lòng nhập một số hợp lệ.");
        }

        return null;
    }

    /// Menu full cho xóa mục  ( xin của Gemini )
    private String pickMenu(){
        CategoryNode rootNode = manager.getTree().getRoot();
        if (rootNode == null) {
            System.out.println("  [LỖI] Không tìm thấy dữ liệu cây tài chính.");
            return null;
        }

        List<CategoryNode> listNodes = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        // Duyệt qua toàn bộ các nhánh trực tiếp của ROOT (gồm cả THU và CHI)
        for (CategoryNode mainBranch : rootNode.getChildren()) {
            buildTreeForAllSelection(mainBranch, 0, listNodes, sb);
        }
        System.out.print(sb.toString());

        System.out.print("\n  Nhập số thứ tự danh mục để chọn (Để trống để hủy): ");
        String inputIndex = readLine().trim();
        if (inputIndex.isEmpty()) {
            return null;
        }

        try {
            int index = Integer.parseInt(inputIndex) - 1; // convert về 0 index
            if (index >= 0 && index < listNodes.size()) {
                CategoryNode selectedNode = listNodes.get(index);
                return selectedNode.getPath(); // Trả về đường dẫn tương ứng
            } else {
                System.out.println("  [LỖI] Số thứ tự lựa chọn không nằm trong danh sách.");
            }
        } catch (NumberFormatException e) {
            System.out.println("  [LỖI] Vui lòng nhập một số hợp lệ.");
        }
        return null;
    }


    //
    // Hàm đệ quy hiển thị sơ đồ cây và tự động gom + đánh số TẤT CẢ các nút
    private void buildTreeForAllSelection(CategoryNode node, int indent, List<CategoryNode> listNodes, StringBuilder sb) {
        listNodes.add(node);
        int displayIndex = listNodes.size();

        // Tạo khoảng thụt lề trực quan
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
        if (indent > 0) {
            sb.append("└── ");
        }

        sb.append("[").append(displayIndex).append("] 📁 ").append(node.getName()).append("\n");

        for (CategoryNode child : node.getChildren()) {
            buildTreeForAllSelection(child, indent + 1, listNodes, sb);
        }
    }

    // Hàm đệ quy hỗ trợ hiển thị và gom các node vào danh sách
    private void buildTreeForSelection(CategoryNode node, int indent, List<CategoryNode> listNodes, StringBuilder sb) {
        listNodes.add(node);
        int displayIndex = listNodes.size();

        // Tạo thụt lề
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
        if (indent > 0) {
            sb.append("└── ");
        }

        sb.append("[").append(displayIndex).append("] 📁 ").append(node.getName()).append("\n");

        for (CategoryNode child : node.getChildren()) {
            buildTreeForSelection(child, indent + 1, listNodes, sb);
        }
    }

    // =========================================================================
    // 2. NHẬP LIỆU GIAO DỊCH
    // =========================================================================
    private void addTransactionMenu() {
        System.out.println("\n  --- THÊM GIAO DỊCH MỚI ---");
        System.out.println("  Danh sách các danh mục lá khả dụng:");
        List<String> leaves = manager.listLeafCategories();
        for (int i = 0; i < leaves.size(); i++) {
            System.out.printf("   [%d] %s\n", i + 1, leaves.get(i));
        }

        System.out.print("  👉 Nhập đường dẫn danh mục (ví dụ: 'THU/Lương' hoặc chọn số thứ tự ở trên): ");
        String pathInput = readLine().trim();
        String categoryPath = "";

        try {
            int index = Integer.parseInt(pathInput);
            if (index >= 1 && index <= leaves.size()) {
                categoryPath = leaves.get(index - 1);
            } else {
                System.out.println("  [LỖI] Số thứ tự lựa chọn vượt quá phạm vi.");
                waitEnter();
                return;
            }
        } catch (NumberFormatException e) {
            categoryPath = pathInput;
        }

        if (categoryPath.isEmpty()) {
            System.out.println("  [LỖI] Đường dẫn danh mục không được để trống.");
            waitEnter();
            return;
        }

        System.out.print("  Nhập số tiền giao dịch (VND): ");
        double amount;
        try {
            amount = Double.parseDouble(readLine().trim());
            if (amount <= 0) {
                System.out.println("  [LỖI] Số tiền giao dịch phải lớn hơn 0.");
                waitEnter();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("  [LỖI] Số tiền nhập vào không phải là số hợp lệ.");
            waitEnter();
            return;
        }

        System.out.print("  Nhập ngày giao dịch (YYYY-MM-DD, nhấn ENTER để chọn ngày hôm nay): ");
        String dateInput = readLine().trim();
        LocalDate date;
        if (dateInput.isEmpty()) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateInput, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("  [LỖI] Định dạng ngày không hợp lệ (phải là YYYY-MM-DD).");
                waitEnter();
                return;
            }
        }

        System.out.print("  Nhập ghi chú giao dịch: ");
        String note = readLine().trim();

        boolean success = manager.addTransaction(amount, date, note, categoryPath);
        if (success) {
            System.out.printf("  [OK] Đã ghi nhận giao dịch thành công vào '%s'.\n", categoryPath);
        } else {
            System.out.println("  [LỖI] Ghi nhận giao dịch thất bại. Hãy chắc chắn đường dẫn danh mục tồn tại.");
        }
        waitEnter();
    }



    // =========================================================================
    // 3. Báo cáo
    // =========================================================================

    private void report(){
        // in trực tiếp ra console
        System.out.print(manager.generateDetailedTextReport());

        // lưu thẳng thành file txt không
        System.out.print("  Bạn có muốn lưu báo cáo này thành file .txt không? (y/n): ");
        String confirm = readLine().trim().toLowerCase();
        if (confirm.equals("y") || confirm.equals("yes")) {
            System.out.print("  Nhập tên file (để trống sẽ lưu là 'baocao.txt'): ");
            String txtFile = readLine().trim();
            if (txtFile.isEmpty()) txtFile = "baocao.txt";

            if (manager.exportDetailedReportToTxt(txtFile)) {
                System.out.println("  [OK] Đã xuất báo cáo dạng bảng thành công ra file: " + txtFile);
            } else {
                System.out.println("  [LỖI] Xuất file thất bại.");
            }
        }
    }


    // =========================================================================
    // 4. LƯU/TẢI DỮ LIỆU
    // =========================================================================
    private void saveLoadMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                 💾 LƯU TRỮ & TẢI DỮ LIỆU                ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  4.1 Lưu dữ liệu hiện tại vào file CSV                   ║");
            System.out.println("║  4.2 Tải lại dữ liệu từ file CSV                         ║");
            System.out.println("║  4.3 Quay lại Menu chính                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.print("  👉 Nhập lựa chọn của bạn (5.1-5.3): ");
            String choice = readLine().trim();
            switch (choice) {
                case "4.1":
                case "1":
                    System.out.print("  Nhập tên file để lưu (để trống sẽ dùng mặc định 'data.csv'): ");
                    String saveFile = readLine().trim();
                    boolean saveSuccess = manager.saveData(saveFile);
                    if (saveSuccess) {
                        System.out.println("  [OK] Đã lưu trữ dữ liệu thành công.");
                    } else {
                        System.out.println("  [LỖI] Lưu trữ thất bại.");
                    }
                    waitEnter();
                    break;
                case "4.2":
                case "2":
                    System.out.print("  Nhập tên file để tải (để trống sẽ dùng mặc định 'data.csv'): ");
                    String loadFile = readLine().trim();
                    boolean loadSuccess = manager.loadData(loadFile);
                    for (String warning : manager.getLoadWarnings()) {
                        System.out.println("  [CẢNH BÁO] " + warning);
                    }
                    if (loadSuccess) {
                        System.out.println("  [OK] Đã tải dữ liệu thành công.");
                    } else {
                        System.out.println("  [LỖI] Tải dữ liệu thất bại hoặc file không có sẵn.");
                    }
                    waitEnter();
                    break;
                case "4.3":
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ.");
                    waitEnter();
            }
        }
    }

    private String readLine() {
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    private void waitEnter() {
        System.out.print("\n  [Nhấn ENTER để tiếp tục...]");
        readLine();
    }
}
