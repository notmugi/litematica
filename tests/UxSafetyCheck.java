import fi.dy.masa.litematica.util.UxSafety.Ticket;

/** Run: JAVA_HOME=/usr/lib/jvm/java-25-openjdk sh tests/check-ux-safety.sh */
public class UxSafetyCheck
{
    public static void main(String[] args)
    {
        Ticket cancelled = new Ticket();
        assert !cancelled.accept(false, () -> { throw new AssertionError("Cancel must not validate or execute"); });
        assert !cancelled.accept(true, () -> true) : "Cancel is terminal";
        Ticket stale = new Ticket();
        assert !stale.accept(true, () -> false) : "Changed context must reject";
        assert !stale.accept(true, () -> true) : "Stale requests cannot be replayed";
        Ticket confirmed = new Ticket();
        int[] checks = {0};
        assert confirmed.accept(true, () -> { ++checks[0]; return true; });
        assert !confirmed.accept(true, () -> { ++checks[0]; return true; });
        assert checks[0] == 1 : "Double click must execute at most once";
        System.out.println("Safety ticket checks passed");
    }
}
