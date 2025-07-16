package happy_sb.profiling.instrument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/11
 */
public class ProfilingTool {

    private static final Logger log = LoggerFactory.getLogger(ProfilingTool.class);

    public String methodB0() {

        List<String> collect = Arrays.asList("111", "222", "333").stream().sorted(String::compareTo).collect(Collectors.toList());

        Supplier<Integer> supplier = new Supplier<Integer>() {
            @Override
            public Integer get() {
                return new Random().nextInt(10);
            }
        };
        if (supplier.get() > 5) {
            log.info("supplier.get() > 5");
        }
        // 提取当前星期几
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

        switch (dayOfWeek) {
            case FRIDAY:
            case TUESDAY:
                byte[] bytes = new byte[1024];
                log.info("星期一二");
                break;
            case MONDAY:
            case THURSDAY:
                String s = "今天星期三";
                log.info("星期三四");
                break;
            default:
                log.info("今天是周末");
        }

        Random random = new Random();
        int times = 0;
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                log.info("{}", random.nextInt());
            }).start();
            times = i;
            break;
        }

        Double aDouble = newDouble();
        if (aDouble > 40) {
            log.info("aDouble > 40");
        }

        try {
            Files.readAllBytes(Paths.get("/Users/jiangjibo/Downloads/start.sh"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return "100";
    }

    private Integer newInt() {
        return new Random().nextInt(100);
    }

    private Double newDouble() {
        return Double.valueOf(new Random().nextInt(100));
    }
}
