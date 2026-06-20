
import java.util.Scanner;
import models.CategoryNode;
import models.Transaction;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static void separator(String title) {
        System.out.println("\n============================================================");
        if (title != null && !title.isEmpty()) {
            System.out.println("   " + title);
            System.out.println("============================================================");
        }
    }


    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║        HỆ THỐNG QUẢN LÝ TÀI CHÍNH CÁ NHÂN (JAVA)         ║");
        System.out.println("║        Môn: Cấu trúc dữ liệu & Giải thuật                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        System.out.println("  1. Mở Giao diện Hệ thống");
        System.out.println("  2. Thoát");
        System.out.print("   👉 Nhập lựa chọn của bạn : ");
        String choice = "";
        if (scanner.hasNextLine()) {
            choice = scanner.nextLine().trim();
        }
        if (choice.equals("1")) {
            new ui.ConsoleMenu().run();
        }
        if (choice.equals("2")) {
            System.out.println("  Tạm biệt!");
        }
    }
}



