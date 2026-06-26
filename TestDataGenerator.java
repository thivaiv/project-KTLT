import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Random;

public class TestDataGenerator {

    private static final Random rd = new Random();

    private static final String[] INCOME = {
            "THU/Lương",
            "THU/Thưởng",
            "THU/Lãi tiết kiệm",
            "THU/Thu nhập khác",
            "THU/BET88"
    };

    private static final String[] EXPENSE = {
            "CHI/Tiền nhà",
            "CHI/Điện nước/Điện",
            "CHI/Điện nước/Nước",
            "CHI/Ăn uống",
            "CHI/Đi lại",
            "CHI/Mua sắm",
            "CHI/Giải trí",
            "CHI/Sức khỏe",
            "CHI/PC"
    };

    private static final String[] NOTES = {
            "",
            "Thanh toán",
            "Online",
            "Tiền mặt",
            "Chuyển khoản",
            "Ví điện tử",
            "Shopee",
            "Lazada",
            "Steam",
            "Grab",
            "Ăn với bạn",
            "Mua linh kiện"
    };

    public static void generate(String fileName, int transactionCount)
            throws IOException {

        try (PrintWriter out = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(fileName),
                        StandardCharsets.UTF_8))) {

            out.println("---TREE---");
            out.println("path,type,budget");

            out.println("THU,THU,0");
            out.println("THU/Lương,THU,0");
            out.println("THU/Thưởng,THU,0");
            out.println("THU/Lãi tiết kiệm,THU,0");
            out.println("THU/Thu nhập khác,THU,0");
            out.println("THU/BET88,THU,0");

            out.println("CHI,CHI,0");
            out.println("CHI/Tiền nhà,CHI,5000000");
            out.println("CHI/Điện nước,CHI,0");
            out.println("CHI/Điện nước/Điện,CHI,2000000");
            out.println("CHI/Điện nước/Nước,CHI,500000");
            out.println("CHI/Ăn uống,CHI,3000000");
            out.println("CHI/Đi lại,CHI,1500000");
            out.println("CHI/Mua sắm,CHI,2000000");
            out.println("CHI/Giải trí,CHI,1000000");
            out.println("CHI/Sức khỏe,CHI,1000000");
            out.println("CHI/PC,CHI,100000000");

            out.println();

            out.println("---TRANSACTIONS---");
            out.println("date,amount,path,note");

            LocalDate start = LocalDate.of(2026, 1, 1);

            for (int i = 0; i < transactionCount; i++) {

                boolean income = rd.nextInt(100) < 25; //25% thu, 75% chi

                String path;
                double amount;

                if (income) {
                    path = INCOME[rd.nextInt(INCOME.length)];
                    amount = (rd.nextInt(20) + 1) * 1_000_000;
                } else {
                    path = EXPENSE[rd.nextInt(EXPENSE.length)];
                    amount = (rd.nextInt(200) + 1) * 10_000;
                }

                LocalDate date = start.plusDays(rd.nextInt(180));

                out.printf("%s,%.2f,%s,%s%n",
                        date,
                        amount,
                        path,
                        NOTES[rd.nextInt(NOTES.length)]);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        generate("data/sample.csv", 100);
        // generate("data/sample.csv", 1000);
        // generate("data/sample.csv", 10000);
    }
}