import com.zstore.api.utils.ThreadUtils;

public class UtilsTest {
    public static void main(String[] args) {
        ThreadUtils.newThread(() -> {
            System.out.println(Thread.currentThread().getName());
        }, "thead", false).start();
    }
}
