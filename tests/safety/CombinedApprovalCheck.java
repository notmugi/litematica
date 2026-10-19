import fi.dy.masa.litematica.util.UxSafety.Ticket;

/** One review, two one-shot stages, cancellation/stale session execute neither. */
public class CombinedApprovalCheck
{
    public static void main(String[] args)
    {
        for (boolean confirm : new boolean[] {false, true})
        for (boolean current : new boolean[] {false, true})
        {
            Ticket review = new Ticket();
            Ticket delete = new Ticket();
            Ticket paste = new Ticket();
            int[] effects = {0, 0};
            Runnable runPaste = () -> { if (paste.accept(true, () -> current)) ++effects[1]; };
            if (review.accept(confirm, () -> current))
            {
                if (delete.accept(true, () -> current)) ++effects[0];
                runPaste.run();
                runPaste.run();
            }
            assert effects[0] == (confirm && current ? 1 : 0);
            assert effects[1] == effects[0] : "One approval covers both stages, never a second prompt";
        }
        Ticket staleContinuation = new Ticket();
        assert !staleContinuation.accept(true, () -> false);
        assert !staleContinuation.accept(true, () -> true) : "Returning to old context cannot replay a rejected continuation";
        System.out.println("Combined approval checks passed");
    }
}
