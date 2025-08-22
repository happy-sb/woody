package happy2b.woody.common.api;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/22
 */
public class WoodyCommand {
    private long time;
    private String eval;
    private String result;
    private boolean success;

    public WoodyCommand(long time, String eval) {
        this.time = time;
        this.eval = eval;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getEval() {
        return eval;
    }

    public void setEval(String eval) {
        this.eval = eval;
    }

    public String getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void result(String result) {
        this.result = result;
        this.success = true;
    }

    public void error(String error) {
        this.result = error;
        this.success = false;
    }
}
